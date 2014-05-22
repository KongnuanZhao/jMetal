//  SynchronousParallelRunner.java
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

package jmetal.util.parallel;

import jmetal.util.Configuration;

import java.util.concurrent.ExecutorService;

/**
 * Created by Antonio J. Nebro on 09/02/14.
 * Abstract class for running tasks in parallel using threads
 */
abstract public class SynchronousParallelRunner {
  protected int numberOfThreads_;
  protected ExecutorService executor_;


  public SynchronousParallelRunner(int threads) {
    numberOfThreads_ = threads;
    if (threads == 0) {
      numberOfThreads_ = Runtime.getRuntime().availableProcessors();
    } else if (threads < 0) {
      Configuration.logger_.severe("SynchronousParallelRunner: the number of threads" +
        " cannot be negative number " + threads);
    } else {
      numberOfThreads_ = threads;
    }
    System.out.println("THREADS: " + numberOfThreads_);
  }

  abstract public void startParallelRunner(Object configuration);

  abstract public void addTaskForExecution(Object[] taskParameters);

  abstract public Object parallelExecution();

  /**
   * Shutdown the executor
   */
  public void stopEvaluator() {
    executor_.shutdown();
  }
}
