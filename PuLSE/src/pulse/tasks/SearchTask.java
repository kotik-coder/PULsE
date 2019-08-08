package pulse.tasks;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import pulse.HeatingCurve;
import pulse.input.ExperimentalData;
import pulse.input.PropertyCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.statements.Problem;
import pulse.properties.BooleanProperty;
import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.search.direction.PathSolver;
import pulse.search.math.ObjectiveFunctionIndex;
import pulse.search.math.Vector;
import pulse.tasks.Status.Details;
import pulse.tasks.listeners.DataCollectionListener;
import pulse.tasks.listeners.StatusChangeListener;
import pulse.tasks.listeners.TaskStateEvent;
import pulse.util.Accessible;
import pulse.util.SaveableDirectory;

public class SearchTask implements Runnable, Accessible, SaveableDirectory {

	private Problem problem;
	private DifferenceScheme scheme;
	private ExperimentalData curve;

	private double timeLimit;
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

	private BooleanProperty[] searchFlags;
	
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
	
	public void updateTimeLimit() {
		final double factor = 1.05;
		timeLimit = factor*curve.timeLimit();
	}
	
	public void reset() {				
		BooleanProperty[] dsFlags = PathSolver.getSearchFlags();
		this.searchFlags		  = new BooleanProperty[dsFlags.length];
		for(int i = 0; i < searchFlags.length; i++) 
			searchFlags[i] = new BooleanProperty(dsFlags[i]);					
		
		rSquared		= Double.NEGATIVE_INFINITY;
		sumOfSquares	= Double.POSITIVE_INFINITY;				
		
		buffer 			= new Buffer();		
		log 			= new Log(this);
			
		this.path		= null;
		this.problem	= null;
		this.scheme		= null;				
		
		final double factor = 1.05;		
		timeLimit =  factor*curve.timeLimit();
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
		
		final double factor = 1.05;		
		timeLimit =  factor*curve.timeLimit();
		testTemperature = (double)curve.getMetadata().getTestTemperature().getValue();
			
		updateThermalProperties();		
	}
	
	public SearchTask() {
		timeLimit 			= (double)NumericProperty.DEFAULT_TIME_LIMIT.getValue();
		testTemperature	= (double)NumericProperty.DEFAULT_T.getValue();
		
		BooleanProperty[] dsFlags = PathSolver.getSearchFlags();
		this.searchFlags		  = new BooleanProperty[dsFlags.length];
		for(int i = 0; i < searchFlags.length; i++) 
			searchFlags[i] = new BooleanProperty(dsFlags[i]);
	
		updateThermalProperties();
		
		rSquared		= Double.NEGATIVE_INFINITY;
		sumOfSquares	= Double.POSITIVE_INFINITY;
		buffer 			= new Buffer();
		
		this.identifier = new Identifier();

		log 			= new Log(this);
		
	}	
	
	public Vector objectiveFunction() {
		return this.getProblem().objectiveFunction(searchFlags);
	}
	
	public void assign(Vector objectiveFunction) {
		this.getProblem().assign(objectiveFunction, searchFlags);
	}
	
	public void updateThermalProperties() {
		PropertyCurve cvCurve = TaskManager.getSpecificHeatCurve();		
		if(cvCurve != null) {
			evalSpecificHeat(cvCurve);
			problem.setSpecificHeat(new NumericProperty(cp, NumericProperty.DEFAULT_CV));
		}
		
		PropertyCurve rhoCurve = TaskManager.getDensityCurve();		
		if(rhoCurve != null) { 
			evalDensity(rhoCurve);
			problem.setDensity(new NumericProperty(rho, NumericProperty.DEFAULT_RHO));
		}
		
		if(rhoCurve != null && cvCurve != null) {
			evalThermalConductivity();
			evalEmissivity();
		}
		
	}
	
	public void solveProblem() {
		scheme.adjustScheme(problem);
		scheme.solve(problem);
	}
	
	public void evalSpecificHeat(PropertyCurve specificHeatCurve) {	
		cp = specificHeatCurve.valueAt(testTemperature); 
	}

	public void evalDensity(PropertyCurve densityCurve) {;		
		rho = densityCurve.valueAt(testTemperature);
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
	
	public ObjectiveFunctionIndex[] parameterTypes() {
		List<ObjectiveFunctionIndex> indices = new ArrayList<ObjectiveFunctionIndex>();
		
		for(BooleanProperty flag : searchFlags) {
			if(! (boolean) flag.getValue() )
				continue; 
			
		indices.add( ObjectiveFunctionIndex.valueOf(flag.getSimpleName()) );
		
		}
		
		return indices.toArray(new ObjectiveFunctionIndex[indices.size()]);
		
	}
	
	public Vector parameterBoundaries() {
		ObjectiveFunctionIndex[] types = this.parameterTypes();
		Vector v = new Vector(types.length);
		double x;
		double lSq = Math.pow((double)problem.getSampleThickness().getValue(), 2);
		
		for(int i = 0; i < types.length; i++) { 
		
			switch(types[i]) {
				case HEAT_LOSSES : x = (double) NumericProperty.DEFAULT_BIOT.getMaximum(); 
				case DIFFUSIVITY : x = (double) NumericProperty.DEFAULT_DIFFUSIVITY.getMaximum()/lSq; break;
				case BASELINE	 : x = (double) NumericProperty.DEFAULT_BASELINE.getMaximum(); break;
				case MAX_TEMP	 : x = (double) NumericProperty.DEFAULT_MAXTEMP.getMaximum(); break;
				default			 : throw new IllegalArgumentException("Type " + types[i] + " unknown"); //$NON-NLS-1$ //$NON-NLS-2$
			}
			
			v.set(i, x);
		}
		
		return v;
			
	}

	public double calculateDeviation() {
		solveProblem();
		return problem.getHeatingCurve().deviationSquares(getExperimentalCurve()); 
	}

	public void adjustScheme() {
		scheme.adjustScheme(problem);
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
	  
	  solveProblem();	  
	  
	  HeatingCurve solutionCurve = this.getProblem().getHeatingCurve();
	  sumOfSquares	   			 = solutionCurve.deviationSquares(curve);
	  
	  PathSolver pathSolver 	= TaskManager.getPathSolver();
	  path 						= new Path(this);
	   
	  double errorTolerance		= (double)PathSolver.getErrorTolerance().getValue();
	  int bufferSize			= (Integer)buffer.getSize().getValue();	  
	  
	  rSquared 					= solutionCurve.rSquared(this.getExperimentalCurve());
	  
	  final double NEGATIVE_LIMIT_MAX = -1E-3;
	  
	  /* search cycle */
	  
	  TaskStateEvent dataCollected = new TaskStateEvent(this, null);
	  List<CompletableFuture<Void>> bufferFutures = new ArrayList<CompletableFuture<Void>>(bufferSize);
	  ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
	  
	  do {				 
		  
		bufferFutures.clear();
		  
		for (int i = 0; i < bufferSize; i++) {			
					
				sumOfSquares = pathSolver.iteration(this);
				
				adjustScheme();
		  		rSquared = solutionCurve.rSquared(getExperimentalCurve());		  				  	
		  		
		  		final int j = i;
		  		
		  		bufferFutures.add(CompletableFuture.runAsync( () -> {
		  			buffer.fill(this, j);		  		
		  			notifyDataListeners(dataCollected); }, singleThreadExecutor ));		  				  		
		  		
		}
		
		bufferFutures.forEach( future -> future.join());
					 
		if( buffer.average(ObjectiveFunctionIndex.HEAT_LOSSES) < NEGATIVE_LIMIT_MAX) 
			  rollback();
	  
	  }  while( buffer.isErrorHigh(errorTolerance) && (status != Status.TERMINATED) );
	  
	  updateThermalProperties();
	  singleThreadExecutor.shutdown();
	 	  	  
	  if( status != Status.TERMINATED )
		 if(rSquared > SUCCESS_CUTOFF)
			 setStatus(Status.DONE);
		 else
			 setStatus(Status.AMBIGUOUS);
	  		
	}
	
	public void rollback() {
		  setSearchFlag(ObjectiveFunctionIndex.HEAT_LOSSES, false);
		  status.setDetails(Details.LIMITED_HEAT_LOSSES);
		  problem.reset(curve);
		  adjustScheme();
		  path.reset(this);
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
	
	public NumericProperty getTimeLimit() {
		return new NumericProperty(timeLimit, NumericProperty.DEFAULT_TIME_LIMIT);
	}
	
	public NumericProperty getTestTemperature() {
		return new NumericProperty(testTemperature, NumericProperty.DEFAULT_T);
	}
	
	public Path getPath() {
		return path;
	}
	
	public NumericProperty getSumOfSquares() {
		return new NumericProperty(Messages.getString("SearchTask.SS"), sumOfSquares); //$NON-NLS-1$
	}
	
	public NumericProperty getRSquared() {
		return new NumericProperty(Messages.getString("SearchTask.R2"), rSquared); //$NON-NLS-1$
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
				
		if(curve != null && problem != null)
			problem.couple(curve);
		
	}
	
	public void setScheme(DifferenceScheme scheme) {
		this.scheme = scheme;
	}
	public void setExperimentalCurve(ExperimentalData curve) {
		this.curve = curve;
		
		if(problem != null) 
			problem.couple(curve);
	}
	
	public void setTimeLimit(NumericProperty timeLimit) {
		this.timeLimit = (double)timeLimit.getValue();
	}
	
	public void setTestTemperature(NumericProperty testTemperature) {
		this.testTemperature = (double)testTemperature.getValue();
		updateThermalProperties();
	}
	
	//override interface method
	
	@Override
	public Property propertyByName(String simpleName) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		Property taskProperty = Accessible.super.propertyByName(simpleName);
		if(taskProperty != null)
			return taskProperty;
		
		Property problemProperty = problem.propertyByName(simpleName);
		if(problemProperty != null)
			return problemProperty;
		
		Property schemeProperty = scheme.propertyByName(simpleName);
		if(schemeProperty != null)
			return schemeProperty;

		Property curveProperty = curve.propertyByName(simpleName);
		if(curveProperty != null)
			return curveProperty;
		
		Property searchProperty = TaskManager.getPathSolver().propertyByName(simpleName);
		if(searchProperty != null)
			return searchProperty;		
	    
	    return null;
		
	}
	
	//override interface method
	
	@Override
	public Object value(String simpleName) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {


		Object taskProperty = Accessible.super.value(simpleName);

		if(taskProperty != null)
			return taskProperty;
	
		Object problemProperty = problem.value(simpleName);
		if(problemProperty != null)
			return problemProperty;
		
		Object schemeProperty = scheme.value(simpleName);
		if(schemeProperty != null)
			return schemeProperty;
		
		Object curveProperty = curve.value(simpleName);
		if(curveProperty != null)
			return curveProperty;
		
		PathSolver pathSolver = TaskManager.getPathSolver();
		
		Object searchProperty = pathSolver.value(simpleName);
		if(searchProperty != null)
			return searchProperty;		
	    
		Object pathProperty = path.value(simpleName);
		if(pathProperty != null)
			return pathProperty;		
	    
		Object pathSolverProperty = pathSolver.value(simpleName);
		if(pathSolverProperty != null)
			return pathSolverProperty;	
		
	    return null;
		
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
		else if(! problem.isReady())
			s.setDetails(Details.INSUFFICIENT_DATA_IN_PROBLEM_STATEMENT);
		else if(scheme == null) 
			s.setDetails(Details.MISSING_DIFFERENCE_SCHEME);
		else if(curve == null)
			s.setDetails(Details.MISSING_HEAT_CURVE);
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
	
	public void setSearchFlag(ObjectiveFunctionIndex id, boolean flag) {
		for(BooleanProperty property : searchFlags)
			if(property.getSimpleName().equals(id.name())) 
				property.setValue(flag);
	}
	
	public int activeFlags() {
		int i = 0;
		for(BooleanProperty bp : searchFlags) {
			boolean b = (boolean) bp.getValue();
			if(b)
				i++;
		}
		
		return i;
		
	}

	public BooleanProperty[] getSearchFlags() {
		return searchFlags;
	}

	public NumericProperty getThermalConductivity() {
		return new NumericProperty(lambda, NumericProperty.DEFAULT_LAMBDA);
	}

	public void setThermalConductivity(NumericProperty lambda) {
		this.lambda = (double)lambda.getValue();
	}	
	
	public NumericProperty getEmissivity() {
		return new NumericProperty(emissivity, NumericProperty.DEFAULT_EMISSIVITY);
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
	};

}