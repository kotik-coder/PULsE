/*
 * Copyright 2022 Artem Lunev <artem.v.lunev@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pulse.problem.schemes.solvers;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.FixedPointIterations;
import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.Problem;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.NONLINEAR_PRECISION;
import pulse.ui.Messages;

/**
 *
 * @author Artem Lunev <artem.v.lunev@gmail.com>
 */
public class ImplicitCoupledSolverNL extends ImplicitCoupledSolver implements FixedPointIterations {

    private double nonlinearPrecision;

    public ImplicitCoupledSolverNL() {
        super();
        nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
        setAutoUpdateFluxes(false);
    }

    public ImplicitCoupledSolverNL(NumericProperty N, NumericProperty timeFactor, NumericProperty timeLimit) {
        super(N, timeFactor, timeLimit);
        nonlinearPrecision = (double) def(NONLINEAR_PRECISION).getValue();
        setAutoUpdateFluxes(false);
    }
    
    @Override
    public void timeStep(final int m) throws SolverException {
        doIterations(getCurrentSolution(), nonlinearPrecision, m);
    }

    @Override
    public void iteration(final int m) throws SolverException {
        super.timeStep(m);
    }

    @Override
    public void finaliseIteration(double[] V) throws SolverException {
        FixedPointIterations.super.finaliseIteration(V);
        var rte = this.getCoupling().getRadiativeTransferEquation();
        setCalculationStatus(rte.compute(V));
    }

    public final NumericProperty getNonlinearPrecision() {
        return derive(NONLINEAR_PRECISION, nonlinearPrecision);
    }

    public final void setNonlinearPrecision(NumericProperty nonlinearPrecision) {
        this.nonlinearPrecision = (double) nonlinearPrecision.getValue();
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        if (type == NONLINEAR_PRECISION) {
            setNonlinearPrecision(property);
        } else {
            super.set(type, property);
        }
    }
   
    @Override
    public DifferenceScheme copy() {
        var grid = getGrid();
        return new ImplicitCoupledSolverNL(grid.getGridDensity(), grid.getTimeFactor(), getTimeLimit());
    }
    
    @Override
    public String toString() {
        return Messages.getString("ImplicitScheme.5");
    }
    
}