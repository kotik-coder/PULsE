package pulse.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import pulse.Baseline;
import pulse.HeatingCurve;
import pulse.input.ExperimentalData;
import pulse.input.InterpolationDataset;
import pulse.input.Metadata;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.direction.PathSolver;
import pulse.search.math.IndexedVector;
import pulse.tasks.Status.Details;
import pulse.tasks.listeners.DataCollectionListener;
import pulse.tasks.listeners.StatusChangeListener;
import pulse.tasks.listeners.TaskStateEvent;
import pulse.ui.Messages;
import pulse.ui.components.PropertyHolderTable;
import pulse.util.Accessible;
import pulse.util.PropertyEvent;
import pulse.util.PropertyHolderListener;
import pulse.util.SaveableDirectory;
import static pulse.properties.NumericPropertyKeyword.*;

public class SearchTask extends Accessible implements Runnable, SaveableDirectory {

	private Problem problem;
	private DifferenceScheme scheme;
	private ExperimentalData curve;

	private double testTemperature;
	private double cp, rho, emissivity, lambda;
	
	private Path   path;
			
	private double rSquared, sumOfSquares;
	
	private Buffer buffer;
	
	private List<DataCollectionListener> listeners = new CopyOnWriteArrayList<DataCollectionListener>();
	private List<StatusChangeListener> statusChangeListeners = new CopyOnWriteArrayList<StatusChangeListener>();
	
	private Status status = Status.INCOMPLETE;
	
	private Log log;
	
	private Identifier identifier;

	private final static double SUCCESS_CUTOFF = 0.2;	

	@Override
	public boolean equals(Object o) {		
		if(! (o instanceof SearchTask))
			return false;
		
		SearchTask other = (SearchTask)o;
		
		if( ! curve.equals(other.getExperimentalCurve()))
			return false;
		
		return true;
	
	}

	public void reset() {				
		rSquared		= (double) NumericProperty.def
				(NumericPropertyKeyword.RSQUARED).getValue();
		sumOfSquares	= (double) NumericProperty.def
				(NumericPropertyKeyword.SUM_OF_SQUARES).getValue();				
		
		buffer 			= new Buffer();
		log 			= new Log(this);
			
		this.path		= null;
		this.problem	= null;
		this.scheme		= null;				
		
		testTemperature = (double)curve.getMetadata().getTestTemperature().getValue();
		
		updateThermalProperties();
		setStatus(Status.INCOMPLETE);
		
		TaskStateEvent e = new TaskStateEvent(this, status);
		notifyStatusListeners(e);
		notifyDataListeners(e);
	}
	
	public SearchTask(ExperimentalData curve) {
		this();
		this.curve = curve;
		curve.setParent(this);
		
		testTemperature = (double)curve.getMetadata().getTestTemperature().getValue();			
		updateThermalProperties();	
		
		final double factor = 1.05;
		
		curve.addDataListener( dataEvent -> {
					if(scheme != null) {
						scheme.setTimeLimit
						(NumericProperty.derive(TIME_LIMIT, factor*curve.timeLimit()));
					}
				}
			);
		
	}
	
	protected SearchTask() {
		testTemperature		= (double)NumericProperty.def(TEST_TEMPERATURE).getValue();

		updateThermalProperties();
		
		rSquared		= Double.NEGATIVE_INFINITY;
		sumOfSquares	= Double.POSITIVE_INFINITY;
		buffer 			= new Buffer();
		
		this.identifier = new Identifier();

		log 			= new Log(this);
					
	}	
	
	public IndexedVector searchVector() {
		return problem.optimisationVector(PathSolver.getSearchFlags());
	}
	
	public void assign(IndexedVector objectiveFunction) {
		problem.assign(objectiveFunction);
	}
	
	public void updateThermalProperties() {
		InterpolationDataset cvCurve = TaskManager.getSpecificHeatCurve();		
		
		if(problem == null)
			return;
		
		if(cvCurve != null) {
			cp = cvCurve.interpolateAt(testTemperature); 
			problem.set(SPECIFIC_HEAT, NumericProperty.derive(SPECIFIC_HEAT, cp));
		}
		
		InterpolationDataset rhoCurve = TaskManager.getDensityCurve();		
		if(rhoCurve != null) { 
			rho = rhoCurve.interpolateAt(testTemperature);
			problem.set(DENSITY, NumericProperty.derive(DENSITY, rho));
		}
		
		if(rhoCurve != null && cvCurve != null) {
			evalThermalConductivity();
			evalEmissivity();
		}
		
	}
	
	public void evalThermalConductivity() {
		double a   = (double)problem.getDiffusivity().getValue();			
		lambda = a * cp * rho;
	}

	public void evalEmissivity() {				
		double Bi     = (double)problem.getHeatLoss().getValue();
		double l      = (double)problem.getSampleThickness().getValue();		
			
		final double sigma = 5.6703E-08; //Stephan-Boltzmann constant
				
		emissivity =  Bi*lambda/(4.*Math.pow(testTemperature, 3)*l*sigma);
	}

	public double calculateDeviation() {
		scheme.solver(problem).solve(problem);
		return problem.getHeatingCurve().deviationSquares(getExperimentalCurve()); 
	}
	
	@Override
	public void run() {
	  switch(status) {
	  	case READY :
	  	case QUEUED : 
	  		setStatus(Status.IN_PROGRESS);
	  		break;
	  	default :
	  		return;
	  }		
	  
	  scheme.solver(problem).solve(problem);	  
	  
	  HeatingCurve solutionCurve = this.getProblem().getHeatingCurve();
	  sumOfSquares	   			 = solutionCurve.deviationSquares(curve);
	  
	  PathSolver pathSolver 	= TaskManager.getPathSolver();
	  
	  path 						= new Path(this);
	   
	  double errorTolerance		= (double)PathSolver.getErrorTolerance().getValue();
	  int bufferSize			= (Integer)buffer.getSize().getValue();	  
	  
	  rSquared 					= solutionCurve.rSquared(this.getExperimentalCurve());	  
	  
	  /* search cycle */
	  
	  TaskStateEvent dataCollected = new TaskStateEvent(this, null);
	  List<CompletableFuture<Void>> bufferFutures = new ArrayList<CompletableFuture<Void>>(bufferSize);
	  ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
  
	  outer: do {				 
		  
		bufferFutures.clear();
		  
		for (int i = 0; i < bufferSize; i++) {	
			
				if(status != Status.IN_PROGRESS)
					break outer;
					
				sumOfSquares = pathSolver.iteration(this);				
		  		rSquared = solutionCurve.rSquared(getExperimentalCurve());		  				  	
		  		
		  		final int j = i;
		  		
		  		bufferFutures.add(CompletableFuture.runAsync( () -> {
		  			buffer.fill(this, j);		  		
		  			notifyDataListeners(dataCollected); }, singleThreadExecutor ));		  				  		
		  		
		}
		
		bufferFutures.forEach( future -> future.join());
	  
	  }  while( buffer.isErrorHigh(errorTolerance) );
	  
	  updateThermalProperties();
	  singleThreadExecutor.shutdown();
	 	  	  
	  if( status == Status.IN_PROGRESS )
		 if(rSquared > SUCCESS_CUTOFF)
			 setStatus(Status.DONE);
		 else
			 setStatus(Status.AMBIGUOUS);
	  		
	}

	public void addTaskListener(DataCollectionListener toAdd) {
		listeners.add(toAdd);
	}
	
	public void addStatusChangeListener(StatusChangeListener toAdd) {
		statusChangeListeners.add(toAdd);
	}
	
	public void removeTaskListeners() {
		listeners.clear();
	}
	
	public void removeStatusChangeListeners() {
		statusChangeListeners.clear();
	}
	
	@Override
	public String toString() {
		return getIdentifier().toString();
	}

	//getters
	
	public Problem getProblem() {
		return problem;
	}
	public DifferenceScheme getScheme() {
		return scheme;
	}
	public ExperimentalData getExperimentalCurve() {
		return curve;
	}
	
	public NumericProperty getTestTemperature() {
		return NumericProperty.derive(TEST_TEMPERATURE, testTemperature);
	}
	
	public Path getPath() {
		return path;
	}
	
	public NumericProperty getSumOfSquares() {
		return NumericProperty.derive(SUM_OF_SQUARES, sumOfSquares); //$NON-NLS-1$
	}
	
	public NumericProperty getRSquared() {
		return NumericProperty.derive(RSQUARED, rSquared); //$NON-NLS-1$
	}
	
	//setters

	public void setRSquared(double rSquared) {
		this.rSquared = rSquared;
	}

	public void setSumOfSquares(double sumOfSquares) {
		this.sumOfSquares = sumOfSquares;
	}
	
	public void setProblem(Problem problem) {
		this.problem = problem;
				
		if(curve == null)
			return;
		
		if(problem == null)
			return;
			
		problem.setParent(this);
		problem.removeListeners(); 
		problem.retrieveData(curve);
		
		problem.addListener(new PropertyHolderListener() {

			@Override
			public void onPropertyChanged(PropertyEvent event) {
				if(!(event.getSource() instanceof Metadata))
					if(!(event.getSource() instanceof PropertyHolderTable))
						return;
				
				problem.estimateSignalRange(curve);
				problem.useParkersSolution(curve);
			
			}

		});
		
		Baseline base = problem.getHeatingCurve().getBaseline();
		base.addListener(event -> base.fitTo(curve) );		
		
	}
	
	public void setScheme(DifferenceScheme scheme) {
		this.scheme = scheme;
		if(scheme != null) {
			scheme.setParent(this);
			final double factor = 1.05;
			scheme.setTimeLimit
			(NumericProperty.derive(TIME_LIMIT, factor*curve.timeLimit()));
		}
	}
	public void setExperimentalCurve(ExperimentalData curve) {
		this.curve = curve;
		
		if(curve != null)		
			curve.setParent(this);
		
		if(problem != null) 
			problem.retrieveData(curve);		
	}
	
	public void setTestTemperature(NumericProperty testTemperature) {
		this.testTemperature = (double)testTemperature.getValue();
		updateThermalProperties();
	}
	
	public Status getStatus() {
		return status;
	}
	
	public void setStatus(Status status) {
		if(this.status == status)
			return;
		
		this.status = status;		
		notifyStatusListeners(new TaskStateEvent(this, status));
	}
		
	public Status checkProblems() {
		if(status == Status.DONE)
			return status;		
		
		PathSolver pathSolver = TaskManager.getPathSolver();				
		Status s = Status.INCOMPLETE;
		
		if(problem == null) 
			s.setDetails(Details.MISSING_PROBLEM_STATEMENT);
		else if(! problem.allDetailsPresent())
			s.setDetails(Details.INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT);
		else if(scheme == null) 
			s.setDetails(Details.MISSING_DIFFERENCE_SCHEME);
		else if(curve == null)
			s.setDetails(Details.MISSING_HEATING_CURVE);
		else if(pathSolver == null)
			s.setDetails(Details.MISSING_PATH_SOLVER);
		else if(PathSolver.getLinearSolver() == null)
			s.setDetails(Details.MISSING_LINEAR_SOLVER);
		else if(buffer == null)
			s.setDetails(Details.MISSING_BUFFER);
		else 
			s = Status.READY;		
			
		setStatus(s);
			
		return status;
	}

	public Identifier getIdentifier() {
		return identifier;
	}
	
	public static boolean compareIds(SearchTask t1, SearchTask t2) {
		return t1.getIdentifier().equals(t2.getIdentifier());
	}
	
	public Log getLog() {
		return log;
	}

	public NumericProperty getThermalConductivity() {
		return NumericProperty.derive(CONDUCTIVITY, lambda);
	}

	public void setThermalConductivity(NumericProperty lambda) {
		this.lambda = (double)lambda.getValue();
	}	
	
	public NumericProperty getEmissivity() {
		return NumericProperty.derive(EMISSIVITY, emissivity);
	}

	public void setEmissivity(NumericProperty emissivity) {
		this.emissivity = (double)emissivity.getValue();
	}
	
	private void notifyDataListeners(TaskStateEvent e) {
		for(DataCollectionListener l : listeners)
  			l.onDataCollected(e);
	}
	
	private void notifyStatusListeners(TaskStateEvent e) {
		for(StatusChangeListener l : statusChangeListeners)
  			l.onStatusChange(e);
	}
	
	@Override
	public String describe() {
		
		StringBuilder sb = new StringBuilder();
		sb.append(TaskManager.getSampleName());
		sb.append("_Task_");
		int extId = curve.getMetadata().getExternalID();
		if(extId < 0)
			sb.append("IntID_" + identifier.getValue());
		else
			sb.append("ExtID_" + extId);
		
		return sb.toString();
		
	}
	
	public void terminate() {
		switch(status) {
			case IN_PROGRESS :
			case QUEUED :
			case READY :
				setStatus(Status.TERMINATED);
				break;
			default :
				return;
			}
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {		
		return;
	}
	
}