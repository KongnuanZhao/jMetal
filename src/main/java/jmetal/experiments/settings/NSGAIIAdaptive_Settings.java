//  NSGAIIAdaptive_Settings.java 
//
//  Authors:
//       Antonio J. Nebro <antonio@lcc.uma.es>
//
//  Copyright (c) 2012 Antonio J. Nebro
//
//  This program is free software: you can redistribute it and/or modify
//  it under the terms of the GNU Lesser General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU Lesser General Public License for more details.
// 
//  You should have received a copy of the GNU Lesser General Public License
//  along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jmetal.experiments.settings;

import jmetal.core.Algorithm;
import jmetal.experiments.Settings;
import jmetal.metaheuristics.nsgaII.NSGAIIAdaptive;
import jmetal.operators.selection.Selection;
import jmetal.operators.selection.SelectionFactory;
import jmetal.problems.ProblemFactory;
import jmetal.util.JMException;
import jmetal.util.offspring.DifferentialEvolutionOffspring;
import jmetal.util.offspring.Offspring;
import jmetal.util.offspring.PolynomialMutationOffspring;
import jmetal.util.offspring.SBXCrossoverOffspring;

import java.util.HashMap;
import java.util.Properties;

/**
 * Settings class of algorithm NSGAIIAdaptive
 * Reference: Antonio J. Nebro, Juan José Durillo, Mirialys Machin Navas, Carlos A. Coello Coello, Bernabé Dorronsoro:
 * A Study of the Combination of Variation Operators in the NSGA-II Algorithm.
 * CAEPIA 2013: 269-278
 * DOI: http://dx.doi.org/10.1007/978-3-642-40643-0_28
 */
public class NSGAIIAdaptive_Settings extends Settings {
  private int populationSize_;
  private int maxEvaluations_;
  private double mutationProbability_;
  private double crossoverProbability_;
  private double mutationDistributionIndex_;
  private double crossoverDistributionIndex_;
  private double cr_;
  private double f_;

  /**
   * Constructor
   *
   * @throws JMException
   */
  public NSGAIIAdaptive_Settings(String problem) throws JMException {
    super(problem);

    Object[] problemParams = {"Real"};
    problem_ = (new ProblemFactory()).getProblem(problemName_, problemParams);

    // Default settings
    populationSize_ = 100;
    maxEvaluations_ = 150000;
    mutationProbability_ = 1.0 / problem_.getNumberOfVariables();
    crossoverProbability_ = 0.9;
    mutationDistributionIndex_ = 20;
    crossoverDistributionIndex_ = 20;
    cr_ = 1.0;
    f_ = 0.5;
  }

  /**
   * Configure NSGAIIAdaptive with user-defined parameter settings
   *
   * @return A NSGAIIAdaptive algorithm object
   * @throws jmetal.util.JMException
   */
  public Algorithm configure() throws JMException {
    Algorithm algorithm;
    Selection selection;

    HashMap<String, Object> parameters = new HashMap<String, Object>();

    algorithm = new NSGAIIAdaptive(problem_);

    // Algorithm parameters
    algorithm.setInputParameter("populationSize", populationSize_);
    algorithm.setInputParameter("maxEvaluations", maxEvaluations_);

    Offspring[] getOffspring = new Offspring[3];
    getOffspring[0] = new DifferentialEvolutionOffspring(cr_, f_);

    getOffspring[1] = new SBXCrossoverOffspring(crossoverProbability_, crossoverDistributionIndex_);

    getOffspring[2] =
      new PolynomialMutationOffspring(mutationProbability_, mutationDistributionIndex_);

    algorithm.setInputParameter("offspringsCreators", getOffspring);

    // Selection Operator 
    parameters = null;
    selection = SelectionFactory.getSelectionOperator("BinaryTournament2", parameters);

    // Add the operators to the algorithm
    algorithm.addOperator("selection", selection);

    return algorithm;
  }

  /**
   * Configure NSGAIIAdaptive with user-defined parameter experiments.settings
   *
   * @return A NSGAIIAdaptive algorithm object
   */
  @Override
  public Algorithm configure(Properties configuration) throws JMException {
    populationSize_ = Integer
      .parseInt(configuration.getProperty("populationSize", String.valueOf(populationSize_)));
    maxEvaluations_ = Integer
      .parseInt(configuration.getProperty("maxEvaluations", String.valueOf(maxEvaluations_)));

    crossoverProbability_ = Double.parseDouble(
      configuration.getProperty("crossoverProbability", String.valueOf(crossoverProbability_)));
    crossoverDistributionIndex_ = Double.parseDouble(configuration
      .getProperty("crossoverDistributionIndex", String.valueOf(crossoverDistributionIndex_)));
    mutationProbability_ = Double.parseDouble(
      configuration.getProperty("mutationProbability", String.valueOf(mutationProbability_)));
    mutationDistributionIndex_ = Double.parseDouble(configuration
      .getProperty("mutationDistributionIndex", String.valueOf(mutationDistributionIndex_)));
    cr_ = Double.parseDouble(configuration.getProperty("CR", String.valueOf(cr_)));
    f_ = Double.parseDouble(configuration.getProperty("F", String.valueOf(f_)));

    return configure();
  }
}
