package simulation_x22168044;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;
import org.uma.jmetal.solution.doublesolution.DoubleSolution;

/**
* Smart car parking simulation 
* @author Shubham Gaur
**/
public class SmartCarParkingSimulation {
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
    static List<Sensor> sensors = new ArrayList<Sensor>();
    static List<Actuator> actuators = new ArrayList<Actuator>();
    static int numOfAreas = 2;
    static int numOfCamerasPerArea = 4;
    static double CAM_TRANSMISSION_TIME = 5;
    private static boolean CLOUD = false;
    private static final double MAX_ALLOWED_ENERGY_CONSUMPTION = 100.0; //energy consumption unit
    private static final double MIN_REQUIRED_EFFICIENCY = 0.8; // a value between 0 and 1 indicating required efficiency
    
    public static void main(String[] args) {
        Log.printLine("Starting Smart Car Parking system...");
        try {
            Log.disable();
            int num_user = 1; // number of cloud users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false; // mean trace events
            CloudSim.init(num_user, calendar, trace_flag);
            String appId = "iss"; // identifier of the application
            FogBroker broker = new FogBroker("broker");
            Application application = createApplication(appId, broker.getId());
            application.setUserId(broker.getId());
            
            //executing pareto multi objective algorithm
            List<DoubleSolution> nsgaIIResults = NSGAIIRunner.NSGAIIExecution();
            
            // Process and use the best NSGA-II result in the simulation
            if (!nsgaIIResults.isEmpty()) {
                DoubleSolution bestNSGAIISolution = getBestSolution(nsgaIIResults);
                processAndUseNSGAIISolution(bestNSGAIISolution);
            }
            
            createFogDevices(broker.getId(), appId);
            Controller controller = null;
            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping
            
            if (CLOUD) {
                // if the mode of deployment is cloud-based
                moduleMapping.addModuleToDevice("image-capture", "cloud"); // placing all instances of Image Capture module in the Cloud
                moduleMapping.addModuleToDevice("object-detection", "cloud"); 
                moduleMapping.addModuleToDevice("alarm-activation", "cloud"); 
            } else {
                for (FogDevice device : fogDevices) {
                    if (device.getName().startsWith("c")) { // names of all Smart Cameras start with 'c'
                        moduleMapping.addModuleToDevice("image-capture", device.getName()); // fixing 1 instance of the Image Capture module to each Smart Camera
                        moduleMapping.addModuleToDevice("object-detection", device.getName()); 
                    }
                    if (device.getName().startsWith("a")) { // names of all fog devices start with 'a'
                        moduleMapping.addModuleToDevice("alarm-activation", device.getName()); 
                    }
                }
            }
            controller = new Controller("master-controller", fogDevices, sensors, actuators);
            
            controller.submitApplication(application,
                    (CLOUD) ? (new ModulePlacementMapping(fogDevices, application, moduleMapping))
                            : (new ModulePlacementEdgewards(fogDevices, sensors, actuators, application, moduleMapping)));
            
            startSimulation();
            
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted errors happen");
        }
	}
	
    private static DoubleSolution getBestSolution(List<DoubleSolution> solutions) {
        // Choose the best solution based on minimum energy consumption
    	return solutions.stream()
    			.min(Comparator.comparingDouble(solution -> solution.objectives()[0]))
    			.orElse(null);
    }
    
    /**
     * Gets the best solution from a list of solutions based on minimum energy consumption.
     *
     * @param solutions A list of solutions to choose from.
     * @return The best solution with minimum energy consumption or null if the list is empty.
     */
    private static void processAndUseNSGAIISolution(DoubleSolution solution) {
        if (solution != null) {
            // Extracting information from the NSGA-II solution
            List<Double> resourceAllocation = solution.variables();
            double energyConsumption = solution.objectives()[0];    // Minimize this value
            double efficiency = solution.objectives()[1];            // Maximize this value
            //update simulation parameters
            updateSimulationParameters(resourceAllocation, energyConsumption, efficiency);
            
        }
    }

    /**
     * Updates simulation parameters based on resource allocation and efficiency.
     *
     * @param resourceAllocation The list of resource allocation values.
     * @param energyConsumption The energy consumption value.
     * @param efficiency The efficiency value.
     */
    private static void updateSimulationParameters(List<Double> resourceAllocation, 
    		double energyConsumption, double efficiency) {
        // Update fog device characteristics based on resource allocation
    	System.out.println(fogDevices.size() +"  "+ resourceAllocation.size());
        if (!resourceAllocation.isEmpty()) {
            
            if (energyConsumption < MAX_ALLOWED_ENERGY_CONSUMPTION) {
                // Decrease transmission time based on reduced energy consumption
                CAM_TRANSMISSION_TIME *= (1 - energyConsumption / MAX_ALLOWED_ENERGY_CONSUMPTION);
            }

            if (efficiency > MIN_REQUIRED_EFFICIENCY) {
                // Increase the number of areas or cameras for improved efficiency
            	numOfAreas += numOfAreas ;
                numOfCamerasPerArea += 20;
            }
            
            if (energyConsumption < MAX_ALLOWED_ENERGY_CONSUMPTION && efficiency > MIN_REQUIRED_EFFICIENCY) {
            	CLOUD = true;
                System.out.println("Switching to cloud-based deployment for energy efficiency.");
            } else {
                CLOUD = false;
                System.out.println("Continuing with edge-based deployment.");
            }
        }
    }
    
    /**
     * Starts the simulation of the smart car parking system.
     */
    private static void startSimulation() {
       try {
            Log.printLine("Starting smart car parking system simulation...");
            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            Log.printLine("Smart car parking simulation finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Error running the simulation");
        }
    }
    
	/**
	 * Creates the fog devices in the physical topology.
	 * @param userId
	 * @param appId
	 */
	private static void createFogDevices(int userId, String appId) {
		if(CLOUD){
			FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25);
			cloud.setParentId(-1);
			fogDevices.add(cloud);
			for(int i=0;i<numOfAreas;i++){
				addArea(i+"", userId, appId, cloud.getId());}
			}
		else{
			FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25);
			cloud.setParentId(-1);
			fogDevices.add(cloud);
			for(int i=0;i<numOfAreas;i++){
				addArea(i+"", userId, appId, cloud.getId());}
			FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
			proxy.setParentId(cloud.getId());
			proxy.setUplinkLatency(100); // latency of connection between proxy server and cloud is 100 ms
			fogDevices.add(proxy);
			for(int i=0;i<numOfAreas;i++){
				addArea(i+"", userId, appId, proxy.getId());}
		}
	}

	private static FogDevice addArea(String id, int userId, String appId, int parentId){
		if(CLOUD){
			FogDevice router = createFogDevice("a-"+id, 2800, 4000, 1000, 10000, 1, 0.0, 107.339, 83.4333);
			fogDevices.add(router);
			router.setUplinkLatency(2); // latency of connection between router and proxy server is 2 ms
			for(int i=0;i<numOfCamerasPerArea;i++){
				String mobileId = id+"-"+i;
				FogDevice camera = addCamera(mobileId, userId, appId, router.getId()); // adding a smart camera to the physical topology. Smart cameras have been modeled as fog devices as well.
				camera.setUplinkLatency(2); // latency of connection between camera and router is 2 ms
				fogDevices.add(camera);
			}
			router.setParentId(parentId);
			return router;
		}
		else{
			FogDevice router = createFogDevice("a-"+id, 2800, 4000, 1000, 10000, 2, 0.0, 107.339, 83.4333);
			fogDevices.add(router);
			router.setUplinkLatency(2); // latency of connection between router and proxy server is 2 ms
			for(int i=0;i<numOfCamerasPerArea;i++){
				String mobileId = id+"-"+i;
				FogDevice camera = addCamera(mobileId, userId, appId, router.getId()); // adding a smart camera to the physical topology. Smart cameras have been modeled as fog devices as well.
				camera.setUplinkLatency(2); // latency of connection between camera and router is 2 ms
				fogDevices.add(camera);
			}
			router.setParentId(parentId);
			return router;
		}
	}
	
	private static FogDevice addCamera(String id, int userId, String appId, int parentId){
		if(CLOUD){
			FogDevice camera = createFogDevice("c-"+id, 500, 1000, 10000, 10000, 2, 0, 87.53, 82.44);
			camera.setParentId(parentId);
			Sensor sensor = new Sensor("s-"+id, "CAMERA", userId, appId, new DeterministicDistribution(CAM_TRANSMISSION_TIME)); // inter-transmission time of camera (sensor) follows a deterministic distribution
			sensors.add(sensor);
			Actuator ptz = new Actuator("ptz-"+id, userId, appId, "alarm-activation");
			actuators.add(ptz);
			sensor.setGatewayDeviceId(camera.getId());
			sensor.setLatency(40.0);  // latency of connection between camera (sensor) and the parent Smart Camera is 1 ms
			ptz.setGatewayDeviceId(parentId);
			ptz.setLatency(1.0);  // latency of connection between PTZ Control and the parent Smart Camera is 1 ms
			return camera;
		}
		else{
			FogDevice camera = createFogDevice("c-"+id, 500, 1000, 10000, 10000, 3, 0, 87.53, 82.44);
			camera.setParentId(parentId);
			Sensor sensor = new Sensor("s-"+id, "CAMERA", userId, appId, new DeterministicDistribution(CAM_TRANSMISSION_TIME)); // inter-transmission time of camera (sensor) follows a deterministic distribution
			sensors.add(sensor);
			Actuator ptz = new Actuator("ptz-"+id, userId, appId, "alarm-activation");
			actuators.add(ptz);
			sensor.setGatewayDeviceId(camera.getId());
			sensor.setLatency(40.0);  // latency of connection between camera (sensor) and the parent Smart Camera is 1 ms
			ptz.setGatewayDeviceId(parentId);
			ptz.setLatency(1.0);  // latency of connection between PTZ Control and the parent Smart Camera is 1 ms
			return camera;
		}
	}
	
	/**
	 * 
	 * @param nodeName name of the device to be used in simulation
	 * @param mips MIPS
	 * @param ram RAM
	 * @param upBw uplink bandwidth
	 * @param downBw downlink bandwidth
	 * @param level hierarchy level of the device
	 * @param ratePerMips cost rate per MIPS used
	 * @param busyPower
	 * @param idlePower
	 * @return
	 */
	private static FogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
		
		List<Pe> peList = new ArrayList<Pe>();

		// 3. Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating
		
		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower)
			);
		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);
		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.0; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN
													// devices by now
		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);
		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}
		fogdevice.setLevel(level);
		return fogdevice;
	}

	/**
	 * Function to create the smart car parking application. 
	 * @param appId unique identifier of the application
	 * @param userId identifier of the user of the application
	 * @return
	 */
	@SuppressWarnings({"serial" })
	private static Application createApplication(String appId, int userId){
		
		Application application = Application.createApplication(appId, userId);
	    /*
	     * Adding modules (vertices) to the application model (directed graph)
	     */
	    application.addAppModule("image-capture", 10);
	    application.addAppModule("object-detection", 10);
	    application.addAppModule("alarm-activation", 10);

	    /*
	     * Connecting the application modules (vertices)
	     */
	    application.addAppEdge("CAMERA", "image-capture", 1000, 500, "CAMERA", Tuple.UP, AppEdge.SENSOR);
	    application.addAppEdge("image-capture", "object-detection", 1000, 500, "detected-objects", Tuple.UP, AppEdge.MODULE);
	    application.addAppEdge("object-detection", "alarm-activation", 100, 28, 100, "ALARM", Tuple.UP, AppEdge.ACTUATOR);

	    application.addTupleMapping("image-capture", "CAMERA", "detected-objects", new FractionalSelectivity(1.0));
	    application.addTupleMapping("object-detection", "detected-objects", "ALARM", new FractionalSelectivity(1.0));

	    final AppLoop loop1 = new AppLoop(new ArrayList<String>() {
	        {
	            add("CAMERA");
	            add("image-capture");
	            add("object-detection");
	            add("alarm-activation");
	        }
	    });

	    List<AppLoop> loops = new ArrayList<AppLoop>() {{add(loop1);}};
	    application.setLoops(loops);

	    return application;
	}
}
