package nuber.students;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * Booking represents the overall "job" for a passenger getting to their destination.
 * 
 * It begins with a passenger, and when the booking is commenced by the region 
 * responsible for it, an available driver is allocated from dispatch. If no driver is 
 * available, the booking must wait until one is. When the passenger arrives at the destination,
 * a BookingResult object is provided with the overall information for the booking.
 * 
 * The Booking must track how long it takes, from the instant it is created, to when the 
 * passenger arrives at their destination. This should be done using Date class' getTime().
 * 
 * Booking's should have a globally unique, sequential ID, allocated on their creation. 
 * This should be multi-thread friendly, allowing bookings to be created from different threads.
 * 
 * @author Yaqi Liu
 *
 */
public class Booking implements Callable<BookingResult>{
	
	private static final AtomicInteger jobCounter = new AtomicInteger(1);
    private final int jobID;
    private final NuberDispatch dispatch;
    private final Passenger passenger;
    private Driver assignedDriver;
    private final long startTime;

		
	/**
	 * Creates a new booking for a given Nuber dispatch and passenger, noting that no
	 * driver is provided as it will depend on whether one is available when the region 
	 * can begin processing this booking.
	 * 
	 * @param dispatch
	 * @param passenger
	 */
	public Booking(NuberDispatch dispatch, Passenger passenger)
	{
		this.dispatch = dispatch;
		dispatch.logEvent(this, "Creating booking");
        this.passenger = passenger;
        this.jobID = jobCounter.getAndIncrement();
        this.startTime = new Date().getTime();
	}
	
	
	/**
	 * At some point, the Nuber Region responsible for the booking can start it (has free spot),
	 * and calls the Booking.call() function, which:
	 * 1.	Asks Dispatch for an available driver
	 * 2.	If no driver is currently available, the booking must wait until one is available. 
	 * 3.	Once it has a driver, it must call the Driver.pickUpPassenger() function, with the 
	 * 			thread pausing whilst as function is called.
	 * 4.	It must then call the Driver.driveToDestination() function, with the thread pausing 
	 * 			whilst as function is called.
	 * 5.	Once at the destination, the time is recorded, so we know the total trip duration. 
	 * 6.	The driver, now free, is added back into Dispatch�s list of available drivers. 
	 * 7.	The call() function the returns a BookingResult object, passing in the appropriate 
	 * 			information required in the BookingResult constructor.
	 *
	 * @return A BookingResult containing the final information about the booking 
	 */
	@Override
	public BookingResult call() throws InterruptedException {
		//dispatch.logEvent(this, "Creating booking");

        // 1. Ask Dispatch for an available driver
		dispatch.logEvent(this, "Starting booking, getting driver");
		
        assignedDriver = dispatch.getDriver();
        
        dispatch.bookingStarted();

        // 2. Log the driver assignment
        

        // 3. Pick up the passenger
        assignedDriver.pickUpPassenger(passenger);
        dispatch.logEvent(this, "Starting, on way to passenger");

        // 4. Drive to the destination
        assignedDriver.driveToDestination();
        dispatch.logEvent(this, "Collected passenger, on way to destination");

        // 5. Calculate the total trip duration
        long tripDuration = System.currentTimeMillis() - startTime;

        // 6. Return the driver back to Dispatch
        dispatch.addDriver(assignedDriver);
        dispatch.logEvent(this, "At destination, driver is now free");

        // 7. Return the BookingResult
        return new BookingResult(jobID, passenger, assignedDriver, tripDuration);
	}
	

	
	/***
	 * Should return the:
	 * - booking ID, 
	 * - followed by a colon, 
	 * - followed by the driver's name (if the driver is null, it should show the word "null")
	 * - followed by a colon, 
	 * - followed by the passenger's name (if the passenger is null, it should show the word "null")
	 * 
	 * @return The compiled string
	 */
	@Override
	public String toString()
	{
		String driverName = (assignedDriver != null) ? assignedDriver.name : "null";
        String passengerName = (passenger != null) ? passenger.name : "null";
        return jobID + ":" + driverName + ":" + passengerName;
	}

}
