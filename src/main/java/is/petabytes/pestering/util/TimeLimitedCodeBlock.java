package is.petabytes.pestering.util;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class TimeLimitedCodeBlock {

	  public static void runWithTimeout(final Runnable runnable, final long timeout, final TimeUnit timeUnit) throws Exception {
	    runWithTimeout(new Callable<Object>() {
	      @Override
	      public Object call() throws Exception {
	        runnable.run();
	        return null;
	      }
	    }, timeout, timeUnit);
	  }

	  public static <T> T runWithTimeout(final Callable<T> callable, final long timeout, final TimeUnit timeUnit) throws Exception {
	    final ExecutorService executor = Executors.newSingleThreadExecutor();
	    final Future<T> future = executor.submit(callable);
	    executor.shutdown(); // This does not cancel the already-scheduled task.
	    try {
	      return future.get(timeout, timeUnit);
	    }
	    catch (final TimeoutException e) {
	      //remove this if you do not want to cancel the job in progress
	      //or set the argument to 'false' if you do not want to interrupt the thread
	      future.cancel(true);
	      throw e;
	    }
	    catch (final ExecutionException e) {
	      //unwrap the root cause
	      final Throwable t = e.getCause();
	      if (t instanceof Error) {
	        throw (Error) t;
	      } else if (t instanceof Exception) {
	        throw (Exception) t;
	      } else {
	        throw new IllegalStateException(t);
	      }
	    }
	  }

	}