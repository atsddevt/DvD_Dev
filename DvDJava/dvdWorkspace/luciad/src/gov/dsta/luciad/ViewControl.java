package gov.dsta.luciad;

import java.awt.Rectangle;
import java.util.List;

import javax.swing.SwingUtilities;

import com.luciad.geodesy.TLcdEllipsoid;
import com.luciad.geodesy.TLcdEllipsoidUtil;
import com.luciad.shape.ILcdPoint;
import com.luciad.shape.shape2D.TLcdXYPoint;
import com.luciad.shape.shape3D.TLcdLonLatHeightPoint;

import gov.dsta.luciad.interfaces.IKGraphic;
import gov.dsta.luciad.interfaces.IKPoint;
import gov.dsta.luciad.interfaces.IViewControl;
import gov.dsta.luciad.interfaces.KImage;

public class ViewControl implements IViewControl{
	private OffScreenGIS offscreenGIS = null;
	@Override
	public double measureDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
		DistanceMeasureController distanceMeasureController = new DistanceMeasureController();
		return distanceMeasureController.measureDistance(new TLcdLonLatHeightPoint(x1, y1, z1), 
				new TLcdLonLatHeightPoint(x2, y2, z2));
		
	}
	@Override
	public void init(){
		offscreenGIS = new OffScreenGIS();
	}
	@Override
	public KImage getCurrentPosition(){
		return offscreenGIS.getCurrentPosition();
	}
	
	@Override
	public List<KImage> getAllObjectOnOffScreen(){
		if(offscreenGIS !=null){
			List<KImage> result = OffScreenGIS.getAllOnScreenObject();
			return result;
		}
		return null;
	}
	@Override
	public double checkAngleBetweenPoint(IKPoint startCoord, IKPoint endCoord) {
		ILcdPoint startPoint = new TLcdXYPoint(startCoord.getX(), startCoord.getY());
		
		ILcdPoint endPoint = new TLcdXYPoint(endCoord.getX(), endCoord.getY());
		
		double[] result = new double[2];
		// The geodesic distance is at index 0
		// The forward azimuth (radians) is at index 1
		TLcdEllipsoidUtil.geodesicDistanceAndForwardAzimuth(startPoint, endPoint, new TLcdEllipsoid(), result);
		double radians = result[1] < Math.PI / 2 ? result[1] + (2 * Math.PI) : result[1];
		return Math.toDegrees(radians);
	}
	
	@Override
	public List<KImage> getAllObject(){
		if(offscreenGIS !=null){
			List<KImage> result = OffScreenGIS.getAllObject();
			return result;
		}
		return null;
	}
	@Override
	public void centerOffScreenGIS(double x, double y , double z , double yaw, double pitch, double roll, double fov){
		if(offscreenGIS !=null){
			OffScreenGIS.centerMap(x, y, z, yaw, pitch, roll, fov);
		}
		
	}
	@Override
	public void showHideOffScreenGIS(boolean isShow){
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if(OffScreenGIS.frame.isVisible() != isShow){
					OffScreenGIS.frame.setVisible(isShow);
					OffScreenGIS.frame.toBack();
				}
				
			}
		});
	}
	
	@Override
	public void deleteIconOnOffscreen(KImage graphic){
		if(offscreenGIS == null){
			return;
		}
		if ((graphic instanceof KImage)) {
			final KImage imgModel = (KImage) graphic;
			OffScreenGIS.removeIcon(imgModel);
		}
	}
	@Override
	public void plotIconOnOffscreen(KImage graphic){
		if(offscreenGIS == null){
			return;
		}
		if ((graphic instanceof KImage)) {
			final KImage imgModel = (KImage) graphic;
			OffScreenGIS.plotIcon(imgModel);
		}
		//OffScreenGIS.getAllOnScreenObject();
	}
	
	
	@Override
	public void setOffScreenBound(Rectangle rect) {
		OffScreenGIS.setBound(rect);
		
	}
	@Override
	public void updateIconOnOffscreen(KImage modelKImage) {
		OffScreenGIS.updateIcon(modelKImage);

		
	}

}
