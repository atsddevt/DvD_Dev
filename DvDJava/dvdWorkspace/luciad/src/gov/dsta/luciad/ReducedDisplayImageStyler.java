package gov.dsta.luciad;



import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.luciad.model.ILcdModel;
import com.luciad.util.collections.TLcdWeakIdentityHashMap;
import com.luciad.view.lightspeed.TLspContext;
import com.luciad.view.lightspeed.style.ALspStyle;
import com.luciad.view.lightspeed.style.styler.ALspStyleCollector;
import com.luciad.view.lightspeed.style.styler.TLspEditableStyler;

import gov.dsta.luciad.interfaces.GisObjectManager;
import gov.dsta.luciad.interfaces.IKGraphic;
import gov.dsta.luciad.interfaces.IKLayer;

public class ReducedDisplayImageStyler extends TLspEditableStyler {

	private static final int DEFAULT_REDUCED_SCALE = 35000;
	
	private Map<Object, List<ALspStyle>> reducedStyleMap = 
			Collections.synchronizedMap(new TLcdWeakIdentityHashMap<Object, List<ALspStyle>>());;
	
	private double reducedScale = DEFAULT_REDUCED_SCALE;
	
	/**
	 * Flag to indicate image does not change according to size.
	 */
	private boolean isIgnoreScaleChange = true;
	
	/**
	 * Flag to indicate to display image in reduce form.
	 */
	private boolean isReduceDisplay = false;
	
	public ReducedDisplayImageStyler() {
//		Hashtable<Object, Object> hashtable = AppConfigReaderFactory.getAppConfigReader().getConfig("settings/map.properties");
//		reducedScale = Double.valueOf(hashtable.get((Object)"reducedScale").toString());
	}
	
	@Override
	public void style(Collection<?> aObjects, ALspStyleCollector aStyleCollector, TLspContext aContext) {

		IKLayer layer = LayerController.getLayersTable().get(aContext.getLayer().getLabel());
		
		Collection<Object> visibleObjects = new ArrayList<Object>();
		
		for (Object object : aObjects) {
			IKGraphic graphic = GisObjectManager.getModel(object);
			if (graphic != null && true) {
				visibleObjects.add(object);
			}
		}
		
		if (layer.isReducedDisplaySupported()) {
			if (isIgnoreScaleChange) {
				if (isReduceDisplay) {
					reduceImage(visibleObjects, aStyleCollector);
				} else {
					super.style(visibleObjects, aStyleCollector, aContext);
				}
			} else if (aContext.getViewXYZWorldTransformation().getScale() >= reducedScale) {
				super.style(visibleObjects, aStyleCollector, aContext);
			} else {
				reduceImage(visibleObjects, aStyleCollector);
			}
		} else {
			
			//check if custom shape
//			if(aObjects.iterator().next() instanceof CustomArrowShape|| aObjects.iterator().next() instanceof RouteShape){
//				 ALspStyleCollector styleCollector = new ALspStyleCollectorWrapper(aStyleCollector) {
//				        @Override
//				        public void submit() {
//				          geometry(new CustomStyleTargetProvider());
//				          super.submit();
//				        }
//				      };    
//				      super.style(aObjects, styleCollector, aContext);
//
//				      if(aObjects.iterator().next() instanceof RouteShape){
//				    	  ((RouteShape) aObjects.iterator().next()).getShapeList();
//				    	 // for(int i = 0 ; i < shapeList.getShapeCount() ; i++){				
//				    		  aStyleCollector
//								.object(aObjects.iterator().next())
//								//.geometry(shapeList.getShape(i))
//								.geometry(new ALspStyleTargetProvider(){
//
//									@Override
//									  public void getStyleTargetsSFCT( Object o, TLspContext aContext, List<Object> aObjects ) {
//										if ( o instanceof RouteShape ) {		
//											  aObjects.add(((RouteShape) o).getShapeList());
//										  }
//										  
//									  }
//
//									@Override
//									public void getEditTargetsSFCT(Object o, TLspContext aContext,
//											List<Object> aObjects) {
//
//									}
//									
//								})
//								.styles(((RouteShape) aObjects.iterator().next()).getReportLineStyle()) // List of styles
//								.submit();
//				    	  	//} 
//				      }
//	    
//			}
//			else{
				super.style(visibleObjects, aStyleCollector, aContext);
		//	}
			
		}
	}	
	
	private void reduceImage(Collection<?> aObjects, ALspStyleCollector aStyleCollector) {
		for(Object o : (Collection<Object>)  aObjects){
			if(o instanceof LonLatPointModel){
				aStyleCollector
					.object(o)
					.styles(reducedStyleMap.get(o)) // List of styles
					.submit();
			}
		}
	}

	public boolean isIgnoreScaleChange() {
		return isIgnoreScaleChange;
	}

	public void setIgnoreScaleChange(boolean isIgnoreScaleChange) {
		this.isIgnoreScaleChange = isIgnoreScaleChange;
	}

	public boolean isReduceDisplay() {
		return isReduceDisplay;
	}

	public void setReduceDisplay(boolean isReduceDisplay) {
		this.isReduceDisplay = isReduceDisplay;
	}

	public void setReducedStyle(ILcdModel aModel, Object aObject, List<ALspStyle> aStyle) {
    	reducedStyleMap.put(aObject, aStyle);
        fireStyleChangeEvent(aModel, Collections.singleton(aObject), null);
    }

}
