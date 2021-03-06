package gov.dsta.luciad.interfaces;

public interface IKPoint  {

	public IKPoint toScreen(double scale, IKPoint basePoint);
	public void copy(IKPoint src);
	public void move(double dx, double dy);

	public double getXInMetre();
	public double getYInMetre();
	
	public double getX();
	public void setX(double x);
	
	public double getY();
	public void setY(double y);
	
	/**
	 * Reserved for future use.
	 * @return
	 */
	public double getZ();
	public void setZ(double z);

}
