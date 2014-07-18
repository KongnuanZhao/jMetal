//  MultithreadedAlgorithmExecutor.java
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

package org.uma.jmetal.util.parallel;

import org.uma.jmetal.core.Algorithm;
import org.uma.jmetal.core.SolutionSet;
import org.uma.jmetal.experiment.Experiment;
import org.uma.jmetal.experiment.Settings;
import org.uma.jmetal.experiment.SettingsFactory;
import org.uma.jmetal.util.Configuration;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Created by Antonio J. Nebro on 09/02/14.
 * Class for executing independent runs of algorithms
 */
public class MultithreadedAlgorithmExecutor implements SynchronousParallelTaskExecutor {
  private Collection<EvaluationTask> taskList_;
  private Experiment experiment_;
  private int numberOfThreads_ ;
  private ExecutorService executor_;

  public MultithreadedAlgorithmExecutor(int threads) {
    numberOfThreads_ = threads;
    if (threads == 0) {
      numberOfThreads_ = Runtime.getRuntime().availableProcessors();
    } else if (threads < 0) {
      Configuration.logger.severe("MultithreadedAlgorithmRunner: the number of threads" +
        " cannot be negative number " + threads);
    } else {
      numberOfThreads_ = threads;
    }
    Configuration.logger.info("THREADS: " + numberOfThreads_);
  }

  public void start(Object experiment) {
    experiment_ = (Experiment) experiment;
    executor_ = Executors.newFixedThreadPool(numberOfThreads_);
    Configuration.logger.info("Cores: " + numberOfThreads_);
    taskList_ = null;
  }

  public void addTask(Object[] taskParameters) {
    if (taskList_ == null) {
      taskList_ = new ArrayList<EvaluationTask>();
    }

    String algorithm = (String) taskParameters[0];
    String problem = (String) taskParameters[1];
    Integer id = (Integer) taskParameters[2];
    taskList_.add(new EvaluationTask(algorithm, problem, id));
  }

  public Object parallelExecution() {
    List<Future<Object>> future = null;
    try {
      future = executor_.invokeAll(taskList_);
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
    taskList_ = null;
    return null;
  }

  public void stop() {
    executor_.shutdown();
  }


  private class EvaluationTask implements Callable<Object> {
    private String problemName_;
    private String algorithmName_;
    private int id_;

    /**
     * Constructor
     *
     * @param problem Problem to solve
     */
    public EvaluationTask(String algorithm, String problem, int id) {
      problemName_ = problem;
      algorithmName_ = algorithm;
      id_ = id;
    }

    public Integer call() throws Exception {
      Algorithm algorithm;
      Object[] settingsParams = {problemName_};
      Settings settings;

      if (experiment_.useConfigurationFilesForAlgorithms()) {
        Properties configuration = new Properties();
        InputStreamReader isr =
          new InputStreamReader(new FileInputStream(algorithmName_ + ".conf"));
        configuration.load(isr);

        String algorithmName = configuration.getProperty("algorithm", algorithmName_);

        settings = (new SettingsFactory()).getSettingsObject(algorithmName, settingsParams);
        algorithm = settings.configure(configuration);
        isr.close();
      } else {
        settings = (new SettingsFactory()).getSettingsObject(algorithmName_, settingsParams);
        algorithm = settings.configure();
      }

      Configuration.logger.info(
        " Running algorithm: " + algorithmName_ + ", problem: " + problemName_ + ", run: " + id_);

      SolutionSet resultFront = algorithm.execute();

      File experimentDirectory;
      String directory;

      directory =
        experiment_.getExperimentBaseDirectory() + "/data/" + algorithmName_ + "/" + problemName_;

      experimentDirectory = new File(directory);
      if (!experimentDirectory.exists()) {
        boolean result = new File(directory).mkdirs();
        Configuration.logger.info("Creating " + directory);
      }

      resultFront.printObjectivesToFile(
        directory + "/" + experiment_.getOutputParetoFrontFileName() + "." + id_);
      resultFront
        .printVariablesToFile(directory + "/" + experiment_.getOutputParetoSetFileName() + "." + id_);

      return id_;
    }
  }

}
