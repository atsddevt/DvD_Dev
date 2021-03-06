package gov.dsta.dvd.dif.video;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.swing.SwingUtilities;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortEvent;
import com.fazecast.jSerialComm.SerialPortMessageListener;

import gov.dsta.dvd.dif.video.model.LatLonHeightPointModel;
import gov.dsta.dvd.dif.video.model.TelemeteryMessage;
import gov.dsta.dvd.dif.video.player.WebcamPlayer;
import gov.dsta.dvd.dif.video.udp.UdpServer;
import gov.dsta.dvd.dif.video.util.FOVUtil;
import gov.dsta.dvd.dif.video.util.ImageConstant;
import gov.dsta.luciad.ViewControl;
import gov.dsta.luciad.interfaces.IKPoint;
import gov.dsta.luciad.interfaces.IViewControl;
import gov.dsta.luciad.interfaces.KImage;
import gov.dsta.luciad.interfaces.KPoint;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

public class Main extends Application {
	private HashMap<String, Label> overlayLabelHashMap = new HashMap<>();

	private List<Label> overlayShownLabelList = new ArrayList<>();
	private List<Label> overlayPrevShownLabelList = new ArrayList<>();
	private static final double _360 = 360;
	private static final double _180= 180;
	private HashMap<String, KImage> targetIdToKImage = new HashMap<>();
	public static WindowedEventRate fpsCounter = new WindowedEventRate(1);
	private static Integer UDP_PORT = 4813;

	private StackPane root = new StackPane();
	private ImageView imageView = new ImageView();
	private AnchorPane overlay = new AnchorPane();
	private double screenCenterX = 0;
	private double screenCenterY = 0;
	private LatLonHeightPointModel ownPosition = new LatLonHeightPointModel(0, 0, 0);
	private static IViewControl viewControl = new ViewControl();
	private static Rectangle bound = new Rectangle();
	//TODO hard coded
	private final static int topLeftX = 0;
	private final static int topLeftY = 0;
	private final static int appWidth = (int) Toolkit.getDefaultToolkit().getScreenSize().getHeight();
	private final static int appHeight = (int) Toolkit.getDefaultToolkit().getScreenSize().getWidth();
	private final static double VFOV = 60; 
	private final static float CAMERA_PITCH_OFF_SET = 0;

	private final static double HFOV_2D = 140; 

	private  final static double HFOV =  FOVUtil.convertVFOVToHFOV(VFOV, appWidth, appHeight); 
	private static UdpServer udpServer = null;
	
	// set this to true to emulate GPS
	private final static boolean emu_GPS =false;
	// set this to true to use webcam
	private final static boolean use_Webcam = true;
	public static boolean doVA = false; //DO VA OR NOT

	//use 3D AR or 2D AR
	private final static boolean AR_2D = true;
	private final static boolean SERIAL_PORT_LISTEN = true;
	public final static Logger logger = Logger.getLogger("");
	private static FileHandler fileTxt ;
	{
		logger.setLevel(Level.INFO);
	}
    public static void main(String[] args) throws SecurityException, IOException {
    	fileTxt = new FileHandler("log.txt");
    	Formatter formatter = new Formatter(){

			@Override
			public String format(LogRecord record) {
				Date date = new Date(record.getMillis());
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss:SSS");
				StringBuilder sb = new StringBuilder();
				sb.append("[");
				sb.append(simpleDateFormat.format(date));
				sb.append("] ");
				sb.append(record.getMessage());
				sb.append("\n");
				return sb.toString();
			}
    		
    	};
    	fileTxt.setFormatter(formatter);
    	logger.addHandler(fileTxt);
    	logger.info("APP STARTING!!!");
    	

    	if(!AR_2D){
    		viewControl.init();
        	viewControl.showHideOffScreenGIS(true);
        	bound.setBounds(topLeftX, topLeftY, appWidth, appHeight);
        	viewControl.setOffScreenBound(bound);
    	}
        launch(args);
    }
  
    
    @Override
    public void start(final Stage primaryStage) throws Exception {
    	///*
    	if(SERIAL_PORT_LISTEN){
    		// initialise the serial port
            System.out.println("\nUsing Library Version v" + SerialPort.getVersion());
    		SerialPort[] ports = SerialPort.getCommPorts();
    		System.out.println("\nAvailable Ports:\n");
    		
    		if (ports.length == 0) {
    			System.out.println("No serial ports detected! Insert serial receiver and run again.");
    			System.exit(0);
    		}
    		
    		for (int i = 0; i < ports.length; ++i){
    			System.out.println("   [" + i + "] " + ports[i].getSystemPortName() + ": " + ports[i].getDescriptivePortName() + " - " + ports[i].getPortDescription());
    		}
    		SerialPort ubxPort;
    		System.out.print("\nChoose your desired serial port or enter -1 to specify a port directly: ");
    		int serialPortChoice = 0;
    		
    		// if there is more than 1 port, ask user to choose
    		if (ports.length > 1) {
    			try {
    				Scanner inputScanner = new Scanner(System.in);
    				serialPortChoice = inputScanner.nextInt();
    				inputScanner.close();
    			} catch (Exception e) {}
    			if (serialPortChoice == -1)
    			{
    				String serialPortDescriptor = "";
    				System.out.print("\nSpecify your desired serial port descriptor: ");
    				try {
    					Scanner inputScanner = new Scanner(System.in);
    					serialPortDescriptor = inputScanner.nextLine();
    					inputScanner.close();
    				} catch (Exception e) {}
    				ubxPort = SerialPort.getCommPort(serialPortDescriptor);
    			}
    			else
    				ubxPort = ports[serialPortChoice];
    		}
    		// else, just open the only port available
    		else {
    			ubxPort = ports[serialPortChoice];
    		}
    		
    		System.out.println("\nPre-setting RTS: " + (ubxPort.setRTS() ? "Success" : "Failure"));
    		boolean openedSuccessfully = ubxPort.openPort(0);
    		System.out.println("\nOpening " + ubxPort.getSystemPortName() + ": " + ubxPort.getDescriptivePortName() + " - " + ubxPort.getPortDescription() + ": " + openedSuccessfully);
    		if (!openedSuccessfully)
    			return;
    		ubxPort.setBaudRate(9600);
    		
    		// add the message listener
    		TelemeteryMessage msg = new TelemeteryMessage();
    		MessageListener listener = new MessageListener(msg);
    		ubxPort.addDataListener(listener);
    	}
    	
		//*/
		// initialise virtual worlds
        overlay.setPickOnBounds(false);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setFullScreen(true);
        root.getChildren().add(imageView);
        
        root.getChildren().add(overlay);

        imageView.fitWidthProperty().bind(primaryStage.widthProperty());
        imageView.fitHeightProperty().bind(primaryStage.heightProperty());
        imageView.fitHeightProperty().addListener((obs,ov,nv)->{
        	updateScreenCenter(imageView.getX(),imageView.getY(),imageView.getFitWidth(),imageView.getFitHeight());
        	
        });
        imageView.fitWidthProperty().addListener((obs,ov,nv)->{
        	updateScreenCenter(imageView.getX(),imageView.getY(),imageView.getFitWidth(),imageView.getFitHeight());

        });
        Scene scene = new Scene(root, appWidth, appHeight);

        primaryStage.setTitle("DVD DIF Video Player");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent ev) {
				Platform.exit();
				System.exit(0);
			}

		});
        WebcamPlayer player = new WebcamPlayer(imageView, 0);
        if (use_Webcam) {
            //TODO uncomment player.start() to start webcam
            player.start();
        }
        
        //TODO testing
        
        Thread t = new Thread(()->{
        	try {
				Thread.sleep(5000);
				addOrUpdateTarget("zk",103.813933, 1.280747, 75);

				setCameraData(103.813933, 1.280757, 75, 0, 0f,0f,(float)VFOV);
			
				while(true){
					Thread.sleep(500);
					
//					KImage image = viewControl.getCurrentPosition();
//					setTestCameraData(image.getLocation().getX(), image.getLocation().getY(), image.getLocation().getZ(), (float)image.getOrientation(), 0f,0f,(float)VFOV);

				}

			} catch (Exception e) {
				e.printStackTrace();
			}

        });
        
        
        //TODO uncomment t.start() to overlay
        t.start();
        
        /*	udpServer
        udpServer = new UdpServer(UDP_PORT).start();
		udpServer.addRequestListener((e->{
			TelemeteryMessage msg = (TelemeteryMessage) e.getCommand();
			setCameraData(msg.getLongitude(), msg.getLatitude(),msg.getAltitude(), msg.getYaw(), calculateCorrectAngleDegree(msg.getPitch()+CAMERA_PITCH_OFF_SET), calculateCorrectAngleDegree(msg.getRoll()),VFOV);
		}));
		*/
        
		try {
			Thread.sleep(5000);
		}
		catch (Exception e){
			e.printStackTrace();
		}
    }
    
	//TODO callback from telemetry module
    public synchronized void addOrUpdateTarget(String id, double lon, double lat, double alt){
    	
		if(!targetIdToKImage.containsKey(id)){
    		KImage entityImage = new KImage();
			entityImage.setId(id);
			IKPoint mapLocation = new KPoint(lon, lat,alt);
			entityImage.setLocation(mapLocation);
			entityImage.setImage(ImageConstant.DRONE);
			entityImage.setSelectedImg(ImageConstant.DRONE);
			if(!AR_2D){
            	viewControl.plotIconOnOffscreen(entityImage);
			}
        	targetIdToKImage.put(id, entityImage);
    	}else{
    		KImage modelKImage = targetIdToKImage.get(id);
    		modelKImage.getLocation().setX(lon);
    		modelKImage.getLocation().setY(lat);
    		modelKImage.getLocation().setZ(alt);
    		if(!AR_2D){
        	viewControl.updateIconOnOffscreen(modelKImage);
    		}
    	}
    	
    	
    }
	private static float calculateCorrectAngleDegree(float degree){
		degree = degree%360;
		if(degree > 180){
			degree = degree - 360;
		}
		//M&S need negative... TODO find out
		//return -degree % _360 ;
		return degree % 360 ;
	}
//	WindowedEventRate teleRate = new WindowedEventRate(1);
	//TODO callback from telemetry module
    public void setCameraData(double x, double y, double z, float yawInDegree, float pitchInDegree, float rollInDegree, double vFov){
//    	logger.info("Lon: " + x + " Lat: " + y + " Alt: " + z + " Yaw: " + yawInDegree + " pitch: " + pitchInDegree + " roll: " + rollInDegree + " hz: " + teleRate.newEvent() );

    	SwingUtilities.invokeLater(()->{
    		
    		double lon, lat, alt;
    		lon = x;
    		lat = y;
    		alt = z;

    		if (emu_GPS) {
    			// test gps coord (DSTA location)
        		lon = 103.816236;
        		lat = 1.280277;
        		alt = 75; //image.getLocation().getZ();
    		}
    		
    		// set position
    		ownPosition.setLon(lon);
        	ownPosition.setLat(lat);
        	ownPosition.setHeight(alt);

        	// set orientation
        	ownPosition.setYaw(yawInDegree);
        	ownPosition.setRoll(rollInDegree);
        	ownPosition.setPitch(pitchInDegree);
        	
        	// set the camera view in the virtual world
        	if(!AR_2D){
        		if (!emu_GPS) {
            		viewControl.centerOffScreenGIS(x, y, z, yawInDegree % 360, pitchInDegree % 360, rollInDegree % 360, vFov);
            	}
            	else {
            		viewControl.centerOffScreenGIS(103.816794, 1.280066, 50, yawInDegree % 360, pitchInDegree % 360, rollInDegree % 360, vFov);
            	}
//    			List<KImage> points = viewControl.getAllObjectOnOffScreen();
            	List<KImage> points = viewControl.getAllObject();
    			updateLabel(points);
        	}else{
        		updateLabel2D(targetIdToKImage.values());
        	}
        	

    	});
	}

   


	private void setTestCameraData(double x, double y, double z, float yawInDegree, float pitchInDegree, float rollInDegree, float vFov){
    	SwingUtilities.invokeLater(()->{
    		ownPosition.setLon(x);
        	ownPosition.setLat(y);
        	ownPosition.setHeight(z);
        	ownPosition.setYaw(yawInDegree % 360);
        	ownPosition.setRoll(rollInDegree % 360);
        	ownPosition.setPitch(pitchInDegree % 360);
//			List<KImage> points = viewControl.getAllObjectOnOffScreen();
        	List<KImage> points = viewControl.getAllObject();
			updateLabel(points);
    	});
	}
    public static float calculateAngle(Point2D point1, Point2D point2){
    	float angle = (float) Math.toDegrees(Math.atan2(point1.getY() - point2.getY(), point1.getX()  - point2.getX()));
    	angle -=90;
    	if(angle < 0){
    		angle += 360;
    	}
    	return angle%360;
    }
    private void relocateLabelOnVideo2D(Label label, String text, double mapAngle) {
  		Platform.runLater(()->{
  			label.setVisible(true);
  			label.setText(text);
  			
  			double width = imageView.getFitWidth();
  			double height = imageView.getFitHeight();
//  			System.err.println(mapAngle + "  " + (HFOV/2));
			//if within FOV
			if((mapAngle < (HFOV_2D/2) && mapAngle>= 0) || (mapAngle > (_360 - (HFOV_2D/2))&& (mapAngle < 360))){
				double layoutX = 0;
				double delta = 0;
				if((mapAngle < (HFOV_2D/2) && mapAngle>= 0) ){
					//right region
					delta = HFOV_2D - ((HFOV_2D/2) - mapAngle);
				}else{
					//left region
					delta =  (HFOV_2D/2) - (360 - mapAngle);
				}
//				System.err.println(mapAngle);
				layoutX = (delta/HFOV_2D)*width;
				label.setGraphic(new ImageView(SwingFXUtils.toFXImage(ImageConstant.DRONE, null)));
				label.setLayoutX(layoutX);
				label.setLayoutY(height/2);
				label.getGraphic().setRotate(0);
			}else{
				label.setGraphic(new ImageView(SwingFXUtils.toFXImage(ImageConstant.RED_ARROW, null)));	
				boolean isLeft = mapAngle > 180;
				if(isLeft){
					label.setLayoutX(0);
					label.setLayoutY(height/2);
					label.getGraphic().setRotate(270);
				}else{
					label.setLayoutX(width-50);
					label.setLayoutY(height/2);
					label.getGraphic().setRotate(90);
				}	
			}
  		});

  	}
    private void relocateLabelOnVideo(Label label, double screenX, double screenY, double z, String text, double mapAngle) {
		Platform.runLater(()->{
			label.setVisible(true);
			label.setText(text);
			
			double width = imageView.getFitWidth();
			double height = imageView.getFitHeight();
//			System.err.println(mapAngle + "  " + (HFOV/2));
			
			if( screenX >= 0 && screenX < width && screenY >= 0 && screenY < height){
				
				//if it is not behind the drone
				if(((mapAngle < (HFOV/2) && mapAngle>= 0) || (mapAngle > (_360 - (HFOV/2))&& (mapAngle < 360)))){
					label.setGraphic(new ImageView(SwingFXUtils.toFXImage(ImageConstant.DRONE, null)));
					label.setLayoutX(screenX-30);
					label.setLayoutY(screenY);
					label.getGraphic().setRotate(0);
				}else{
					label.setGraphic(new ImageView(SwingFXUtils.toFXImage(ImageConstant.RED_ARROW, null)));	
					double x2 = 0;
					double y2 = 0;
					double x3 = screenX;
					double y3 = screenY;
//					if(ownPosition.getHeight() < z){
//						y3 = y3 + 10000;
//					}else{
//						y3 = y3 - 10000;
//					}
					if(mapAngle < 180){
						//more to right
						x3 = x3 + 10000;
					}else{
						//more to left
						x3 = x3 - 10000;
					}
					
					if(x3 <0){
						x2 = 0;//label.getWidth();
					}else if(x3 > width){
						x2 = width - 50;//label.getWidth();
					}else{
						x2 = x3;
					}
					if(y3<0){
						y2 = 0 ;//label.getHeight();
					}else if(y3 >height ){
						y2 = height - 50;//label.getHeight();
					}else{
						y2 = y3;
					}
					label.setLayoutX(x2);
					label.setLayoutY(y2);
					double angle = calculateAngle(new Point2D(screenCenterX, screenCenterY), new Point2D(x2, y2));
					label.getGraphic().setRotate(angle);
					
				}
				
				
				

			}else{
				label.setGraphic(new ImageView(SwingFXUtils.toFXImage(ImageConstant.RED_ARROW, null)));				
				
				double x2 = 0;
				double y2 = 0;
				if(screenX <0){
					x2 = 0;//label.getWidth();
				}else if(screenX > width){
					x2 = width - 50;//label.getWidth();
				}else{
					x2 = screenX;
				}
				if(screenY<0){
					y2 = 0 ;//label.getHeight();
				}else if(screenY >height ){
					y2 = height - 50;//label.getHeight();
				}else{
					y2 = screenY;
				}
				
				label.setLayoutX(x2);
				label.setLayoutY(y2);
				double angle = calculateAngle(new Point2D(screenCenterX, screenCenterY), new Point2D(screenX, screenY));

				label.getGraphic().setRotate(angle);
			}
			
		});

	}
    private void updateScreenCenter(double x, double y, double width, double height){
    	Point2D point = new Point2D(x+(width/2), y+(height/2));
    	screenCenterX = point.getX();
    	screenCenterY = point.getY();
    }
    public void drawLabel(Label label){
    	Platform.runLater(()->{
			overlay.getChildren().add(label);
		});
    }
    public static double correctDegree(double degree){
		double degree2 = degree % _360;
		while(degree2 < 0 ){
			degree2 += _360;
		}
		return degree2;
	}
    public void updateLabel(List<KImage> points){
    	overlayPrevShownLabelList.addAll(overlayShownLabelList);
		overlayShownLabelList.clear();
		if(points != null && !points.isEmpty()){
			points.forEach(point->{
				double x = point.getScreenX();
				double y = point.getScreenY();
				Label storedLabel =  overlayLabelHashMap.get(point.getId());
				if(storedLabel ==null){
					storedLabel = new Label();
					storedLabel.setTextFill(Color.WHITE);
					storedLabel.setContentDisplay(ContentDisplay.BOTTOM);
					BackgroundFill bgFill = new BackgroundFill(Color.color(Color.BLACK.getRed(),Color.BLACK.getGreen(),Color.BLACK.getBlue(), 0.05), CornerRadii.EMPTY, Insets.EMPTY);
					Background bg = new Background(bgFill);
					storedLabel.setBackground(bg);
					drawLabel(storedLabel);
					overlayLabelHashMap.put(point.getId(), storedLabel);
				}
				
				double mapAngle = viewControl.checkAngleBetweenPoint(new KPoint(ownPosition.getLon(), ownPosition.getLat()), new KPoint(point.getLocation().getX(),point.getLocation().getY()))%360;

				mapAngle = correctDegree(mapAngle - ownPosition.getYaw());
				
				double distance = calculateDistance(ownPosition, new LatLonHeightPointModel(point.getLocation().getX(),point.getLocation().getY(),point.getLocation().getZ()));
				relocateLabelOnVideo(storedLabel, x, y, point.getLocation().getZ(),""+distance + "m", mapAngle);
				overlayShownLabelList.add(storedLabel);

			});
		}	
    }
    private void updateLabel2D(Collection<KImage> points) {
    	overlayPrevShownLabelList.addAll(overlayShownLabelList);
		overlayShownLabelList.clear();
		if(points != null && !points.isEmpty()){
			points.forEach(point->{
				Label storedLabel =  overlayLabelHashMap.get(point.getId());
				if(storedLabel ==null){
					storedLabel = new Label();
					storedLabel.setTextFill(Color.WHITE);
					storedLabel.setContentDisplay(ContentDisplay.BOTTOM);
					BackgroundFill bgFill = new BackgroundFill(Color.color(Color.BLACK.getRed(),Color.BLACK.getGreen(),Color.BLACK.getBlue(), 0.4), CornerRadii.EMPTY, Insets.EMPTY);
					Background bg = new Background(bgFill);
					storedLabel.setBackground(bg);
					drawLabel(storedLabel);
					overlayLabelHashMap.put(point.getId(), storedLabel);
				}
				
				double mapAngle = viewControl.checkAngleBetweenPoint(new KPoint(ownPosition.getLon(), ownPosition.getLat()), new KPoint(point.getLocation().getX(),point.getLocation().getY()))%360;
				mapAngle = correctDegree(mapAngle - ownPosition.getYaw());
				double distance = calculateDistance(ownPosition, new LatLonHeightPointModel(point.getLocation().getX(),point.getLocation().getY(),point.getLocation().getZ()));
				relocateLabelOnVideo2D(storedLabel,""+distance + "m", mapAngle);
				overlayShownLabelList.add(storedLabel);

			});
		}	
		
	}


	private double calculateDistance(LatLonHeightPointModel firstPoint,
			LatLonHeightPointModel secondPoint) {
		int result = (int) (viewControl.measureDistance(firstPoint.getLon(), firstPoint.getLat(), firstPoint.getHeight(),
				secondPoint.getLon(), secondPoint.getLat(), secondPoint.getHeight()) * 10);
		return result/ 10.0;
		
	}
	
	
	private final class MessageListener implements SerialPortMessageListener
	{
		TelemeteryMessage payload;
		public MessageListener (TelemeteryMessage msg) {
			this.payload = msg;
		}
		public String byteToHex(byte num)
		{
			char[] hexDigits = new char[2];
			hexDigits[0] = Character.forDigit((num >> 4) & 0xF, 16);
			hexDigits[1] = Character.forDigit((num & 0xF), 16);
			return new String(hexDigits);
		}
		@Override
		public int getListeningEvents() { return SerialPort.LISTENING_EVENT_DATA_RECEIVED; }
		@Override
		public void serialEvent(SerialPortEvent event)
		{
			byte[] byteArray = event.getReceivedData();
			//StringBuffer hexStringBuffer = new StringBuffer();
			//for (int i = 0; i < byteArray.length; i++)
			//	hexStringBuffer.append(byteToHex(byteArray[i]));
			//System.out.println("Number of hex bytes: " + hexStringBuffer.length() * 0.5);
			//System.out.println("Received the following message: " + hexStringBuffer.toString());
			//System.out.println("Received the following message: " + byteArray);
			
			ByteArrayInputStream bytearrypacket = new ByteArrayInputStream(byteArray);
			DataInputStream dataInputpacket = new DataInputStream(bytearrypacket);
			try{
				// discard start headers
				dataInputpacket.readShort();
				
				// read msg id
				int id;
				id = Byte.toUnsignedInt(dataInputpacket.readByte());
				
				// attitude message
				if (id == 30)
				{
					this.payload.read_Attitude(dataInputpacket);
				}
				
				// gps message
				if (id == 33) 
				{
					this.payload.read_GPS(dataInputpacket);
				}
				
				setCameraData(this.payload.getLongitude(), 
						this.payload.getLatitude(), 
						this.payload.getAltitude(), 
						this.payload.getYaw_DEG(), 
						calculateCorrectAngleDegree(this.payload.getPitch_DEG()+CAMERA_PITCH_OFF_SET), 
						calculateCorrectAngleDegree(this.payload.getRoll_DEG()),
						VFOV);

			}catch(Exception e){
				e.printStackTrace();
			}
			
		}
		@Override
		public byte[] getMessageDelimiter() { return new byte[]{ (byte)0x7D, (byte)0x7D }; }
		@Override
		public boolean delimiterIndicatesEndOfMessage() { return false; }
	}
	
	

  

}