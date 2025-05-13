package simulation_x22168044;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.uma.jmetal.problem.doubleproblem.impl.AbstractDoubleProblem;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.errorchecking.JMetalException;

/**
 * Class representing problem FogProblem
 * @author Shubham Gaur
 */
public class FogProblem extends AbstractDoubleProblem {

	/** Constructor. Creates default instance of problem ZDT1 (30 decision variables) */
	@SuppressWarnings("serial")
	public FogProblem() {
		this(5, 2); // 5 variables and 2 objectives
	}

	public FogProblem(Integer numberOfVariables, Integer numberOfObjectives) throws JMetalException {
		numberOfObjectives(numberOfObjectives);
		name("FogProblem");

		List<Double> lowerLimit = new ArrayList<>(numberOfVariables);
		List<Double> upperLimit = new ArrayList<>(numberOfVariables);

		IntStream.range(0, numberOfVariables).forEach(i -> {
			lowerLimit.add(0.0);
			upperLimit.add(1.0);
		});

		variableBounds(lowerLimit, upperLimit);
	}

	/**
	 * Evaluate() method
	 */
	public DoubleSolution evaluate(DoubleSolution solution) {
		int numberOfVariables = numberOfVariables();
		int numberOfObjectives = solution.objectives().length;

		double[] f = new double[numberOfObjectives];
		double[] x = new double[numberOfVariables];

		for (int i = 0; i < numberOfVariables; i++) {
            x[i] = solution.variables().get(i);
        }
		
		// fog-related metrics
		double detectionAccuracy = calculateDetectionAccuracy(x);
        double resourceUtilization = calculateResourceUtilization(x);
        double energyConsumption = calculateEnergyConsumption(x);
        
		// Objectives
        f[0] = calculateModifiedEnergyConsumption(energyConsumption, resourceUtilization); // Minimize energy consumption
        f[1] = calculateEfficiency(detectionAccuracy, resourceUtilization); // Maximize efficiency

        
        IntStream.range(0, numberOfObjectives).forEach(i -> solution.objectives()[i] = f[i]);

        // constraint: Maximum resource utilization constraint
        double maxResourceUtilization = 0.8;
        double resourceUtilizationConstraintViolation = Math.max(0.0, resourceUtilization - maxResourceUtilization);
        solution.attributes().put("ResourceUtilizationConstraintViolation", resourceUtilizationConstraintViolation);
        
        return solution;
	}
	
	private double calculateDetectionAccuracy(double[] x) {
	    // Placeholder logic: Sum of normalized variables as an example
	    return IntStream.range(0, x.length).mapToDouble(i -> x[i]).sum();
	}

	private double calculateResourceUtilization(double[] x) {
	    // Placeholder logic: Product of normalized variables as an example
	    return IntStream.range(0, x.length).mapToDouble(i -> x[i]).reduce(1, (a, b) -> a * b);
	}

	private double calculateEnergyConsumption(double[] x) {
	    // Placeholder logic: Square of the sum of normalized variables as an example
	    return Math.pow(IntStream.range(0, x.length).mapToDouble(i -> x[i]).sum(), 2);
	}
	
	private double calculateModifiedEnergyConsumption(double energyConsumption, double resourceUtilization) {
        // Modify the energy consumption calculation based on resource utilization
        return energyConsumption / resourceUtilization;
    }
	
	private double calculateEfficiency(double detectionAccuracy, double resourceUtilization) {
        // Placeholder logic: A combination of detection accuracy and resource utilization as an example
        return detectionAccuracy * resourceUtilization;
    }
}
