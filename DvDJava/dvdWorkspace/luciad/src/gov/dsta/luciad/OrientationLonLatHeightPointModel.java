package gov.dsta.luciad;

import com.luciad.util.ILcd3DOrientationSettable;

public class OrientationLonLatHeightPointModel extends LonLatHeightPointModel implements ILcd3DOrientationSettable {
//	protected Long id;

	  private double fYaw = 0;
	  private double fPitch = 0;
	  private double fRoll = 0;

	  public OrientationLonLatHeightPointModel(double aX, double aY, double aZ, double aYaw, double aPitch, double aRoll) {
	    super(aX, aY, aZ);
	    fYaw = aYaw;
	    fPitch = aPitch;
	    fRoll = aRoll;
	  }
	  public OrientationLonLatHeightPointModel(double aX, double aY, double aZ) {
		    super(aX, aY, aZ);
		   
	  }
	  @Override
	  public double getPitch() {
	    return fPitch;
	  }

	  @Override
	  public double getRoll() {
	    return fRoll;
	  }

	  @Override
	  public double getOrientation() {
	    return fYaw;
	  }

	  @Override
	  public void setPitch(double aPitch) {
	    fPitch = aPitch;
	  }

	  @Override
	  public void setRoll(double aRoll) {
	    fRoll = aRoll;
	  }

	  @Override
	  public void setOrientation(double aYaw) {
	    fYaw = aYaw;
	  }
	@Override
	public String toString() {
		return "OrientationLonLatHeightPointModel [id=" + id + ", fYaw=" + fYaw + ", fPitch=" + fPitch + ", fRoll="
				+ fRoll + ", getX()=" + getX() + ", getY()=" + getY() + ", getZ()=" + getZ() + "]";
	}
	
	
	
	  
}
