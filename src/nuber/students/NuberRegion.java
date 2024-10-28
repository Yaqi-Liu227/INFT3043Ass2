package nuber.students;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

/**
 * A single Nuber region that operates independently of other regions, other than getting 
 * drivers from bookings from the central dispatch.
 * 
 * A region has a maxSimultaneousJobs setting that defines the maximum number of bookings 
 * that can be active with a driver at any time. For passengers booked that exceed that 
 * active count, the booking is accepted, but must wait until a position is available, and 
 * a driver is available.
 * 
 * Bookings do NOT have to be completed in FIFO order.
 * 
 * @author Yaqi Liu
 *
 */
public class NuberRegion {
	
	private final NuberDispatch dispatch;
    private final String regionName;
    private final Semaphore availableSlots;
    private final ExecutorService executorService;
    private final LinkedBlockingQueue<Booking> pendingBookings;
    private volatile boolean shutdown;

	
	/**
	 * Creates a new Nuber region
	 * 
	 * @param dispatch The central dispatch to use for obtaining drivers, and logging events
	 * @param regionName The regions name, unique for the dispatch instance
	 * @param maxSimultaneousJobs The maximum number of simultaneous bookings the region is allowed to process
	 */
	public NuberRegion(NuberDispatch dispatch, String regionName, int maxSimultaneousJobs)
	{
		this.dispatch = dispatch;
        this.regionName = regionName;
        this.availableSlots = new Semaphore(maxSimultaneousJobs);
        this.executorService = Executors.newCachedThreadPool();
        this.pendingBookings = new LinkedBlockingQueue<>();
        this.shutdown = false;
	}
	
	/**
	 * Creates a booking for given passenger, and adds the booking to the 
	 * collection of jobs to process. Once the region has a position available, and a driver is available, 
	 * the booking should commence automatically. 
	 * 
	 * If the region has been told to shutdown, this function should return null, and log a message to the 
	 * console that the booking was rejected.
	 * 
	 * @param waitingPassenger
	 * @return a Future that will provide the final BookingResult object from the completed booking
	 */
	public synchronized Future<BookingResult> bookPassenger(Passenger waitingPassenger)
	{		
		if (shutdown) {
            //dispatch.logEvent(null, regionName + ": Rejected booking due to shutdown.");
            return null;
        }

        // Create a new booking
        Booking booking = new Booking(dispatch, waitingPassenger);
        //dispatch.logEvent(booking, "Booking created for passenger " + waitingPassenger.name);

        return executorService.submit(() -> {
            try {
                availableSlots.acquire();

                //dispatch.logEvent(booking, "Booking started for passenger " + waitingPassenger.name);
                BookingResult result = booking.call();

                //dispatch.logEvent(booking, "Booking completed for passenger " + waitingPassenger.name);
                return result;
            } catch (InterruptedException e) {
                e.printStackTrace();
                return null;
            } finally {
                availableSlots.release();
            }
        });
	}
	
	/**
	 * Called by dispatch to tell the region to complete its existing bookings and stop accepting any new bookings
	 */
	public void shutdown()
	{
        this.shutdown = true; 
        dispatch.logEvent(null, regionName + ": Region shutting down.");
        executorService.shutdown();
	}

}
