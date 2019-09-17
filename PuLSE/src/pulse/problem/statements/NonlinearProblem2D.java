package pulse.problem.statements;

import static pulse.properties.NumericPropertyKeyword.DIAMETER;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS_SIDE;
import static pulse.properties.NumericPropertyKeyword.PYROMETER_SPOT;

import java.util.List;
import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.search.math.IndexedVector;
import pulse.tasks.TaskManager;
import pulse.ui.Messages;

public class NonlinearProblem2D extends NonlinearProblem implements TwoDimensional {
	
	private double d, Bi3, dAv;
	
	public NonlinearProblem2D() {
		super();
		Bi3 = (double)NumericProperty.def(HEAT_LOSS_SIDE).getValue();
		d 	= (double)NumericProperty.def(DIAMETER).getValue();
		dAv = (double)NumericProperty.def(PYROMETER_SPOT).getValue();
	}
	
	public NonlinearProblem2D(Problem p) {
		super(p);
		Bi3 = (double)NumericProperty.def(HEAT_LOSS_SIDE).getValue();
		d 	= (double)NumericProperty.def(DIAMETER).getValue();
		dAv = (double)NumericProperty.def(PYROMETER_SPOT).getValue();
	}
	
	public NonlinearProblem2D(Problem2D p) {
		super(p);
		Bi3 = p.Bi3;
		d 	= p.d;
		dAv = p.dAv;
	}

	public NonlinearProblem2D(NumericProperty a, NumericProperty cV, NumericProperty rho, NumericProperty qAbs, NumericProperty T) {
		super(a, cV, rho, qAbs, T);
		Bi3 = (double)NumericProperty.def(HEAT_LOSS_SIDE).getValue();
		d 	= (double)NumericProperty.def(DIAMETER).getValue();
		dAv = (double)NumericProperty.def(PYROMETER_SPOT).getValue();
	}
	
	@Override
	public List<Property> listedParameters() {
		List<Property> list = super.listedParameters();
		list.addAll(super.listedParameters());
		list.add(NumericProperty.def(HEAT_LOSS_SIDE));
		list.add(NumericProperty.def(DIAMETER));
		list.add(NumericProperty.def(PYROMETER_SPOT));
		return list;
	}
	
	public void assign(IndexedVector params) {
		super.assign(params);
		
		for(int i = 0, size = params.dimension(); i < size; i++) {
			
			switch( params.getIndex(i) ) {
				case HEAT_LOSS			:	Bi3 = params.get(i); break;
				default 				: 	continue;
			}
		}
		
	}
	
	@Override
	public String toString() {
		return Messages.getString("NonlinearProblem2D.Descriptor"); //$NON-NLS-1$
	}
	
	@Override
	public NumericProperty getPyrometerSpot() {
		return NumericProperty.derive(PYROMETER_SPOT, dAv); //$NON-NLS-1$
	}

	public void setPyrometerSpot(NumericProperty dAv) {
		this.dAv = (double)dAv.getValue();
	}
	
	public NumericProperty getSideLosses() {
		return NumericProperty.derive(HEAT_LOSS_SIDE, Bi3);
	}

	public void setSideLosses(NumericProperty bi3) {
		this.Bi3 = (double)bi3.getValue();
	}
	
	@Override
	public NumericProperty getSampleDiameter() {
		return NumericProperty.derive(DIAMETER, d);
	}

	public void setSampleDiameter(NumericProperty d) {
		this.d = (double)d.getValue();
	}
	
	@Override
	public boolean allDetailsPresent() {
		if(TaskManager.getDensityCurve() == null || TaskManager.getSpecificHeatCurve() == null)
			return false;
		return super.allDetailsPresent();
	}
	
}