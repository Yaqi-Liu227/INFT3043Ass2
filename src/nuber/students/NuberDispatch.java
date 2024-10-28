package nuber.students;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * The core Dispatch class that instantiates and manages everything for Nuber
 * 
 * @author Yaqi Liu
 *
 */
public class NuberDispatch {

	/**
	 * The maximum number of idle drivers that can be awaiting a booking 
	 */
	private final int MAX_DRIVERS = 999;
	
	private boolean logEvents = false;
	
	private final HashMap<String, NuberRegion> regions = new HashMap<>();
    private final Queue<Driver> driverQueue = new LinkedList<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    protected int bookingsAwaitingDriver = 0;
    
	
	/**
	 * Creates a new dispatch objects and instantiates the required regions and any other objects required.
	 * It should be able to handle a variable number of regions based on the HashMap provided.
	 * 
	 * @param regionInfo Map of region names and the max simultaneous bookings they can handle
	 * @param logEvents Whether logEvent should print out events passed to it
	 */
	public NuberDispatch(HashMap<String, Integer> regionInfo, boolean logEvents)
	{
		this.logEvents = logEvents;
        // Initialize regions
        for (String regionName : regionInfo.keySet()) {
            int maxJobs = regionInfo.get(regionName);
            regions.put(regionName, new NuberRegion(this, regionName, maxJobs));
        }
	}
	
	/**
	 * Adds drivers to a queue of idle driver.
	 *  
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @param The driver to add to the queue.
	 * @return Returns true if driver was added to the queue
	 */
	public synchronized boolean addDriver(Driver newDriver)
	{
		if (driverQueue.size() < MAX_DRIVERS) {
            driverQueue.offer(newDriver);
            notifyAll();
            //System.out.println("Driver added to dispatch: " + newDriver.name);
            return true;
        }
        return false;
	}
	
	/**
	 * Gets a driver from the front of the queue
	 *  
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @return A driver that has been removed from the queue
	 * @throws InterruptedException 
	 */
	public synchronized Driver getDriver() throws InterruptedException
	{
		while (driverQueue.isEmpty()) {
	        wait();
	    }
		Driver driver = driverQueue.poll();
		//System.out.println("Driver dispatched: " + driver.name);
	    return driver;
	}

	/**
	 * Prints out the string
	 * 	    booking + ": " + message
	 * to the standard output only if the logEvents variable passed into the constructor was true
	 * 
	 * @param booking The booking that's responsible for the event occurring
	 * @param message The message to show
	 */
	public void logEvent(Booking booking, String message) {
		
		if (!logEvents) return;
		
		System.out.println(booking + ": " + message);
		
	}

	/**
	 * Books a given passenger into a given Nuber region.
	 * 
	 * Once a passenger is booked, the getBookingsAwaitingDriver() should be returning one higher.
	 * 
	 * If the region has been asked to shutdown, the booking should be rejected, and null returned.
	 * 
	 * @param passenger The passenger to book
	 * @param region The region to book them into
	 * @return returns a Future<BookingResult> object
	 */
	public Future<BookingResult> bookPassenger(Passenger passenger, String region) {
		NuberRegion nuberRegion = regions.get(region);
        if (nuberRegion != null) {
            // Ensure the region is not shut down and can process the booking
            Future<BookingResult> future = nuberRegion.bookPassenger(passenger);
            if (future != null) {
                bookingsAwaitingDriver++; // Increment the count of bookings awaiting drivers
                //logEvent(null, "Creating booking");
                //System.out.print("\n" + bookingsAwaitingDriver + "\n");
                return future;
            }
            // Log a message when booking fails (for example, due to shutdown)
            //logEvent(null, "Booking rejected for passenger in region: " + region);
        } else {
            logEvent(null, "Region not found for booking: " + region);
        }
        return null;
	}

	/**
	 * Gets the number of non-completed bookings that are awaiting a driver from dispatch
	 * 
	 * Once a driver is given to a booking, the value in this counter should be reduced by one
	 * 
	 * @return Number of bookings awaiting driver, across ALL regions
	 */
	public int getBookingsAwaitingDriver()
	{
		return bookingsAwaitingDriver;
	}
	
	public synchronized void bookingStarted() {
        bookingsAwaitingDriver--;
    }
	
	/**
	 * Tells all regions to finish existing bookings already allocated, and stop accepting new bookings
	 */
	public void shutdown() {
		for (NuberRegion region : regions.values()) {
            region.shutdown();
        }
        executorService.shutdown();
	}
	
}
