//  MultithreadedExperimentExecutor.java
//
//  Authors:
//       Antonio J. Nebro <antonio@lcc.uma.es>
//
//  Copyright (c) 2014 Antonio J. Nebro
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
//

package org.uma.jmetal.experiment.result;

import org.uma.jmetal.core.Algorithm;
import org.uma.jmetal.core.SolutionSet;
import org.uma.jmetal.experiment.ExperimentData;
import org.uma.jmetal.experiment.Settings;
import org.uma.jmetal.experiment.SettingsFactory;
import org.uma.jmetal.util.Configuration;
import org.uma.jmetal.util.parallel.SynchronousParallelTaskExecutor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Created by Antonio J. Nebro on 18/07/14.
 * Class for executing independent runs of algorithms
 */
public class MultithreadedExperimentExecutor implements SynchronousParallelTaskExecutor {
  private Collection<EvaluationTask> taskList;
  private ExperimentExecution experimentExecution;
  private int numberOfThreads;
  private ExecutorService executor;

  /** Constructor */
  public MultithreadedExperimentExecutor(int threads) {
    numberOfThreads = threads;
    if (threads == 0) {
      numberOfThreads = Runtime.getRuntime().availableProcessors();
    } else if (threads < 0) {
      Configuration.logger.severe("MultithreadedExperimentExecutor: the number of threads" +
        " cannot be negative number " + threads);
    } else {
      numberOfThreads = threads;
    }
    Configuration.logger.info("THREADS: " + numberOfThreads);
  }

  public void start(Object object) {
    experimentExecution = (ExperimentExecution)object ;
    executor = Executors.newFixedThreadPool(numberOfThreads);
    Configuration.logger.info("Cores: " + numberOfThreads);
    taskList = null;
  }

  public void addTask(Object[] taskParameters) {
    if (taskList == null) {
      taskList = new ArrayList<EvaluationTask>();
    }

    String algorithm = (String) taskParameters[0];
    String problem = (String) taskParameters[1];
    Integer id = (Integer) taskParameters[2];
    ExperimentData experimentData = (ExperimentData) taskParameters[3] ;
    taskList.add(new EvaluationTask(algorithm, problem, id, experimentData));
  }

  public Object parallelExecution() {
    List<Future<Object>> future = null;
    try {
      future = executor.invokeAll(taskList);
    } catch (InterruptedException e1) {
      Configuration.logger.log(Level.SEVERE, "Error", e1);
    }
    List<Object> resultList = new Vector<Object>();

    for (Future<Object> result : future) {
      Object returnValue = null;
      try {
        returnValue = result.get();
        resultList.add(returnValue);
      } catch (InterruptedException e) {
        Configuration.logger.log(Level.SEVERE, "Error", e);
      } catch (ExecutionException e) {
        Configuration.logger.log(Level.SEVERE, "Error", e);
      }
    }
    taskList = null;
    return null;
  }

  public void stop() {
    executor.shutdown();
  }

  /** Class defining the tasks to be computed in parallel */
  private class EvaluationTask implements Callable<Object> {
    private String problemName;
    private String algorithmName;
    private int id;
    private ExperimentData experimentData ;

    /** Constructor */
    public EvaluationTask(String algorithm, String problem, int id, ExperimentData experimentData) {
      problemName = problem;
      algorithmName = algorithm;
      this.id = id;
      this.experimentData = experimentData ;
    }

    public Integer call() throws Exception {
      Algorithm algorithm;
      Object[] settingsParams = {problemName};
      Settings settings;

      if (experimentExecution.useAlgorithmConfigurationFiles()) {
        Properties configuration = new Properties();
        InputStreamReader isr =
          new InputStreamReader(new FileInputStream(algorithmName + ".conf"));
        configuration.load(isr);

        String algorithmName = configuration.getProperty("algorithm", this.algorithmName);

        settings = (new SettingsFactory()).getSettingsObject(algorithmName, settingsParams);
        algorithm = settings.configure(configuration);
        isr.close();
      } else {
        settings = (new SettingsFactory()).getSettingsObject(algorithmName, settingsParams);
        algorithm = settings.configure();
      }

      Configuration.logger.info(
        " Running algorithm: " + algorithmName + ", problem: " + problemName + ", run: " + id);

      SolutionSet resultFront = algorithm.execute();

      File experimentDirectory;
      String directory;

      directory =
        experimentData.getExperimentBaseDirectory() + "/data/" + algorithmName + "/" + problemName;

      experimentDirectory = new File(directory);
      if (!experimentDirectory.exists()) {
        boolean result = new File(directory).mkdirs();
        Configuration.logger.info("Creating " + directory);
      }

      resultFront.printObjectivesToFile(
        directory + "/" + experimentExecution.getParetoFrontFileName() + "." + id);
      resultFront
        .printVariablesToFile(directory + "/" + experimentExecution.getParetoSetFileName() + "." + id);

      return id;
    }
  }
}
