package simulation_x22168044;

import org.uma.jmetal.algorithm.multiobjective.nsgaii.*;
import org.uma.jmetal.algorithm.Algorithm;
import org.uma.jmetal.algorithm.examples.AlgorithmRunner;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.ProblemFactory;
import org.uma.jmetal.qualityindicator.QualityIndicatorUtils;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.AbstractAlgorithmRunner;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.SolutionListUtils;
import org.uma.jmetal.util.VectorUtils;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;

import java.io.IOException;
import java.util.List;

/** Class to configure and run the NSGA-II algorithm 
 * 
 * @author Shubham Gaur
 * */
public class NSGAIIRunner extends AbstractAlgorithmRunner {

	/**
	 * @param args Command line arguments.
	 * @throws java.io.IOException
	 * @throws JMetalException
	 * @throws ClassNotFoundException
	 */
	public static List<DoubleSolution> NSGAIIExecution() throws JMetalException, IOException {

		/// FogSimulation/src/simulation_x22168044/MyProblem.java
		String problemName = "simulation_x22168044.FogProblem";
		String referenceParetoFront = "ZDT1.csv";

		Problem<DoubleSolution> problem = ProblemFactory.<DoubleSolution>loadProblem(problemName);
		//Problem<DoubleSolution> problem = new FogProblem();

		double crossoverProbability = 0.9;
		double crossoverDistributionIndex = 20.0;
		CrossoverOperator<DoubleSolution> crossover = new SBXCrossover(crossoverProbability,
				crossoverDistributionIndex);

		double mutationProbability = 1.0 / problem.numberOfVariables();
		double mutationDistributionIndex = 20.0;
		MutationOperator<DoubleSolution> mutation = new PolynomialMutation(mutationProbability,
				mutationDistributionIndex);

		SelectionOperator<List<DoubleSolution>, DoubleSolution> selection = new BinaryTournamentSelection<>(
				new RankingAndCrowdingDistanceComparator<>());

		int populationSize = 100;
		Algorithm<List<DoubleSolution>> algorithm = new NSGAIIBuilder<>(problem, crossover, mutation, populationSize)
				.setSelectionOperator(selection).setMaxEvaluations(25000).build();

		AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();

		List<DoubleSolution> population = algorithm.result();
		long computingTime = algorithmRunner.getComputingTime();
		
		int i=0;
		// Print the solutions or process them further
		for (DoubleSolution solution : population) {
		    // Interpretation for a hypothetical fog surveillance problem
		    List<Double> resourceAllocation = solution.variables();  // These could be percentages or specific values
		    double energyConsumption = solution.objectives()[0];     // Minimize this value
		    double efficiency = solution.objectives()[1];             // Maximize this value
		}
		
		JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");

		printFinalSolutionSet(population);
		QualityIndicatorUtils.printQualityIndicators(SolutionListUtils.getMatrixWithObjectiveValues(population),
				VectorUtils.readVectors(referenceParetoFront, ","));
		
		return population;
	}
}
