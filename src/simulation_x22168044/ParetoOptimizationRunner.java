package simulation_x22168044;

import org.uma.jmetal.algorithm.multiobjective.nsgaiii.NSGAIIIBuilder;
import org.uma.jmetal.algorithm.multiobjective.nsgaii.*;
import org.uma.jmetal.algorithm.examples.AlgorithmRunner;
import org.uma.jmetal.operator.crossover.CrossoverOperator;
import org.uma.jmetal.operator.crossover.impl.SBXCrossover;
import org.uma.jmetal.operator.mutation.MutationOperator;
import org.uma.jmetal.operator.mutation.impl.PolynomialMutation;
import org.uma.jmetal.operator.selection.SelectionOperator;
import org.uma.jmetal.operator.selection.impl.BinaryTournamentSelection;
import org.uma.jmetal.problem.Problem;
import org.uma.jmetal.problem.ProblemFactory;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;
import org.uma.jmetal.util.AbstractAlgorithmRunner;
import org.uma.jmetal.util.errorchecking.JMetalException;
import org.uma.jmetal.util.JMetalLogger;
import org.uma.jmetal.util.comparator.RankingAndCrowdingDistanceComparator;
import org.uma.jmetal.util.fileoutput.SolutionListOutput;
import org.uma.jmetal.util.fileoutput.impl.DefaultFileOutputContext;

import java.util.List;

/** Class to configure and run the NSGA-III algorithm 
 * @author Shubham Gaur
 * */
public class ParetoOptimizationRunner extends AbstractAlgorithmRunner {
  /**
   * @param args Command line arguments.
   * @throws java.io.IOException
   * @throws SecurityException
   * @throws ClassNotFoundException Usage: three options -
   *     org.uma.jmetal.runner.multiobjective.nsgaii.NSGAIIIRunner -
   *     org.uma.jmetal.runner.multiobjective.nsgaii.NSGAIIIRunner problemName -
   *     org.uma.jmetal.runner.multiobjective.nsgaii.NSGAIIIRunner problemName paretoFrontFile
   */
  public static void main(String[] args) throws JMetalException {
   
    ///FogSimulation/src/simulation_x22168044/MyProblem.java
    String problemName = "simulation_x22168044.FogProblem";

    //Problem<DoubleSolution> problem = ProblemFactory.<DoubleSolution>loadProblem(problemName);
    Problem<DoubleSolution> problem = new FogProblem();
    
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

    var algorithm =
        new NSGAIIIBuilder<>(problem)
            .setCrossoverOperator(crossover)
            .setMutationOperator(mutation)
            .setSelectionOperator(selection)
            .setMaxIterations(10000)
            .setNumberOfDivisions(12)
            .build();
    
//    NSGAII<DoubleSolution> algorithm1 = new NSGAIIBuilder<>()
//            .setMaxEvaluations(10000)
//            .setPopulationSize(100)
//            .build();

    AlgorithmRunner algorithmRunner = new AlgorithmRunner.Executor(algorithm).execute();

    List<DoubleSolution> solutions = algorithm.result();
    long computingTime = algorithmRunner.getComputingTime();
    
    // Print the solutions or process them further
    for (DoubleSolution solution : solutions) {
        System.out.println("Variables: " + solution.variables());
        System.out.println("Objectives: " + solution.objectives());
        System.out.println("---");
    }
    
    new SolutionListOutput(solutions)
        .setVarFileOutputContext(new DefaultFileOutputContext("VAR.csv", ","))
        .setFunFileOutputContext(new DefaultFileOutputContext("FUN.csv", ","))
        .print();

    JMetalLogger.logger.info("Total execution time: " + computingTime + "ms");
    JMetalLogger.logger.info("Algorithm Runner: " + solutions.toString());
    JMetalLogger.logger.info("Objectives values have been written to file FUN.csv");
    JMetalLogger.logger.info("Variables values have been written to file VAR.csv");
  }
}
