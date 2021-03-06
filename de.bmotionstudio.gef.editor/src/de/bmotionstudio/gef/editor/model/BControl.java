/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package de.bmotionstudio.gef.editor.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.ui.views.properties.IPropertySource;

import de.bmotionstudio.gef.editor.AttributeConstants;
import de.bmotionstudio.gef.editor.BMotionEditorPlugin;
import de.bmotionstudio.gef.editor.IBControlService;
import de.bmotionstudio.gef.editor.attribute.AbstractAttribute;
import de.bmotionstudio.gef.editor.attribute.BAttributeCoordinates;
import de.bmotionstudio.gef.editor.attribute.BAttributeCustom;
import de.bmotionstudio.gef.editor.attribute.BAttributeHeight;
import de.bmotionstudio.gef.editor.attribute.BAttributeID;
import de.bmotionstudio.gef.editor.attribute.BAttributeSize;
import de.bmotionstudio.gef.editor.attribute.BAttributeVisible;
import de.bmotionstudio.gef.editor.attribute.BAttributeWidth;
import de.bmotionstudio.gef.editor.attribute.BAttributeX;
import de.bmotionstudio.gef.editor.attribute.BAttributeY;
import de.bmotionstudio.gef.editor.internal.Animation;
import de.bmotionstudio.gef.editor.internal.BControlPropertySource;
import de.bmotionstudio.gef.editor.observer.IObserverListener;
import de.bmotionstudio.gef.editor.observer.Observer;
import de.bmotionstudio.gef.editor.scheduler.SchedulerEvent;

/**
 * 
 * A Control is a graphical representation of some aspects of the model.
 * Typically we use labels, images or buttons to represent informations. For
 * instance, if we model a system that has a temperature and a threshold
 * temperature that triggers a cool down, we might simply use two labels
 * displaying both values, or maybe we can incorporate both information into a
 * gauge display. It is also possible to define new controls for domain specific
 * visualizations.
 * 
 * @author Lukas Ladenberger
 * 
 */
public abstract class BControl implements IAdaptable, Cloneable {

	/** The type of the control (e.g. label, button ...) */
	protected String type;

	private transient Rectangle layout = null;

	private transient Point location = null;

	private BControlList children;

	private Map<String, Observer> observers;

	private Map<String, SchedulerEvent> events;

	private Map<String, AbstractAttribute> attributes;

	/**
	 * Since the parent is set via the method readResolve(), we mark the
	 * variable as transient
	 */
	private transient BControl parent;

	private transient Visualization visualization;

	private transient PropertyChangeSupport listeners;

	private transient ArrayList<IObserverListener> observerListener;

	private BMotionGuide verticalGuide, horizontalGuide;

	/** List of outgoing Connections. */
	private List<BConnection> sourceConnections;
	/** List of incoming Connections. */
	private List<BConnection> targetConnections;

	public static final transient String PROPERTY_LAYOUT = "NodeLayout";
	public static final transient String PROPERTY_LOCATION = "NodeLocation";
	public static final transient String PROPERTY_ADD = "NodeAddChild";
	public static final transient String PROPERTY_REMOVE = "NodeRemoveChild";
	public static final transient String PROPERTY_RENAME = "NodeRename";
	/** Property ID to use when the list of outgoing connections is modified. */
	public static final String SOURCE_CONNECTIONS_PROP = "BMS.SourceConn";
	/** Property ID to use when the list of incoming connections is modified. */
	public static final String TARGET_CONNECTIONS_PROP = "BMS.TargetConn";

	public BControl(Visualization visualization) {
		this.visualization = visualization;
		this.children = new BControlList();
		this.observers = new HashMap<String, Observer>();
		this.events = new HashMap<String, SchedulerEvent>();
		this.attributes = new HashMap<String, AbstractAttribute>();
		this.listeners = new PropertyChangeSupport(this);
		this.observerListener = new ArrayList<IObserverListener>();
		this.sourceConnections = new ArrayList<BConnection>();
		this.targetConnections = new ArrayList<BConnection>();
		init();
	}

	protected Object readResolve() {
		// Populate parent
		for (BControl child : getChildrenArray())
			child.setParent(this);
		return this;
	}

	/**
	 * Remove an incoming or outgoing connection from this shape.
	 * 
	 * @param conn
	 *            a non-null connection instance
	 * @throws IllegalArgumentException
	 *             if the parameter is null
	 */
	public void removeConnection(BConnection conn) {
		if (conn == null) {
			throw new IllegalArgumentException();
		}
		if (conn.getSource() == this) {
			getSourceConnections().remove(conn);
			getListeners().firePropertyChange(SOURCE_CONNECTIONS_PROP, null,
					conn);
		} else if (conn.getTarget() == this) {
			getTargetConnections().remove(conn);
			getListeners().firePropertyChange(TARGET_CONNECTIONS_PROP, null,
					conn);
		}
	}

	/**
	 * Add an incoming or outgoing connection to this shape.
	 * 
	 * @param conn
	 *            a non-null connection instance
	 * @throws IllegalArgumentException
	 *             if the connection is null or has not distinct endpoints
	 */
	public void addConnection(BConnection conn) {
		if (conn == null || conn.getSource() == conn.getTarget()) {
			throw new IllegalArgumentException();
		}
		conn.setVisualization(getVisualization());
		if (conn.getSource() == this) {
			getSourceConnections().add(conn);
			getListeners().firePropertyChange(SOURCE_CONNECTIONS_PROP, null,
					conn);
		} else if (conn.getTarget() == this) {
			getTargetConnections().add(conn);
			getListeners().firePropertyChange(TARGET_CONNECTIONS_PROP, null,
					conn);
		}
	}

	private void init() {

		// Init ID
		String ID;
		if (this instanceof Visualization)
			ID = "visualization";
		else if (visualization == null)
			ID = UUID.randomUUID().toString();
		else
			ID = (visualization.getMaxIDString(type));
		initAttribute(new BAttributeID(ID), AbstractAttribute.ROOT);

		// Init location and dimension attributes
		BAttributeCoordinates coordinatesAtr = new BAttributeCoordinates(null);
		initAttribute(coordinatesAtr, AbstractAttribute.ROOT);
		initAttribute(new BAttributeX(100), coordinatesAtr);
		initAttribute(new BAttributeY(100), coordinatesAtr);

		BAttributeSize sizeAtr = new BAttributeSize(null);
		initAttribute(sizeAtr, AbstractAttribute.ROOT);
		initAttribute(new BAttributeWidth(100), sizeAtr);
		initAttribute(new BAttributeHeight(100), sizeAtr);

		// Init visible and this attribute
		initAttribute(new BAttributeVisible(true), AbstractAttribute.ROOT);
		initAttribute(new BAttributeCustom(""), AbstractAttribute.ROOT);

		// Init custom control attributes
		initAttributes();

	}

	protected abstract void initAttributes();

	public String getID() {
		return getAttributeValue(AttributeConstants.ATTRIBUTE_ID).toString();
	}

	public AbstractAttribute getAttribute(String attributeID) {
		return getAttributes().get(attributeID);
	}

	public Object getAttributeValue(String attributeID) {

		AbstractAttribute atr = attributes.get(attributeID);

		if (atr != null) {
			return atr.getValue();
		} else {
			// TODO: handle error/exception (no such attribute)
			return null;
		}

	}

	public boolean setAttributeValue(String attributeID, Object value) {
		return setAttributeValue(attributeID, value, true);
	}

	public boolean setAttributeValue(String attributeID, Object value,
			Boolean firePropertyChange) {

		AbstractAttribute atr = attributes.get(attributeID);

		if (atr == null) {
			return false;
			// TODO: Throw some error!?!
		}

		atr.setControl(this);

		if ((atr.getValue() != null && atr.getValue().equals(value))
				|| !atr.isEditable())
			return true;

		atr.setValue(value, firePropertyChange);

		return true;

	}

	public void restoreDefaultValue(String attributeID) {
		AbstractAttribute atr = attributes.get(attributeID);
		if (atr != null) {
			atr.restoreValue();
			Object oldVal = atr.getValue();
			Object initValue = atr.getInitValue();
			getListeners().firePropertyChange(attributeID, oldVal, initValue);
		}
	}

	public boolean hasAttribute(String attributeID) {
		return attributes.containsKey(attributeID);
	}

	public void setLayout(Rectangle newLayout) {
		Rectangle oldLayout = getLayout();
		layout = newLayout;
		setAttributeValue(AttributeConstants.ATTRIBUTE_X, newLayout.x, false);
		setAttributeValue(AttributeConstants.ATTRIBUTE_Y, newLayout.y, false);
		setAttributeValue(AttributeConstants.ATTRIBUTE_WIDTH, newLayout.width,
				false);
		setAttributeValue(AttributeConstants.ATTRIBUTE_HEIGHT,
				newLayout.height, false);
		getListeners()
				.firePropertyChange(PROPERTY_LAYOUT, oldLayout, newLayout);
	}

	public Rectangle getLayout() {
		int width = Integer.valueOf(getAttributeValue(
				AttributeConstants.ATTRIBUTE_WIDTH).toString());
		int height = Integer.valueOf(getAttributeValue(
				AttributeConstants.ATTRIBUTE_HEIGHT).toString());
		int x = Integer.valueOf(getAttributeValue(
				AttributeConstants.ATTRIBUTE_X).toString());
		int y = Integer.valueOf(getAttributeValue(
				AttributeConstants.ATTRIBUTE_Y).toString());
		if (layout == null) {
			layout = new Rectangle(x, y, width, height);
		} else {
			layout.x = x;
			layout.y = y;
			layout.width = width;
			layout.height = height;
		}
		return layout;
	}

	public void setLocation(Point newLocation) {
		Point oldLocation = getLocation();
		location = newLocation;
		setAttributeValue(AttributeConstants.ATTRIBUTE_X, newLocation.x, false);
		setAttributeValue(AttributeConstants.ATTRIBUTE_Y, newLocation.y, false);
		getListeners().firePropertyChange(PROPERTY_LOCATION, oldLocation,
				newLocation);
	}

	public Point getLocation() {
		int x = Integer.valueOf(getAttributeValue(
				AttributeConstants.ATTRIBUTE_X).toString());
		int y = Integer.valueOf(getAttributeValue(
				AttributeConstants.ATTRIBUTE_Y).toString());
		if (location == null) {
			location = new Point(x, y);
		} else {
			location.x = x;
			location.y = y;
		}
		return location;
	}

	public Dimension getDimension() {
		int width = Integer.valueOf(getAttributeValue(
				AttributeConstants.ATTRIBUTE_WIDTH).toString());
		int height = Integer.valueOf(getAttributeValue(
				AttributeConstants.ATTRIBUTE_HEIGHT).toString());
		return new Dimension(width, height);
	}

	public void addChild(BControl child) {
		addChild(child, -1);
	}

	public void addChild(BControl child, int index) {
		child.setParent(this);
		if (index >= 0) {
			children.add(index, child);
		} else {
			children.add(child);
		}
		getListeners().firePropertyChange(PROPERTY_ADD, index, child);
	}

	public boolean removeChild(BControl child) {
		boolean b = children.remove(child);
		if (b)
			getListeners().firePropertyChange(PROPERTY_REMOVE, child, null);
		return b;
	}

	public List<BControl> getChildrenArray() {
		if (children == null)
			children = new BControlList();
		return children;
	}

	public void setChildrenArray(BControlList children) {
		this.children = children;
	}

	public boolean hasChildren() {
		return children.size() > 0;
	}

	public BControl getChild(String ID) {
		for (BControl bcontrol : children) {
			String bcontrolID = bcontrol.getAttributeValue(
					AttributeConstants.ATTRIBUTE_ID).toString();
			if (bcontrolID != null) {
				if (bcontrolID.equals(ID))
					return bcontrol;
			}
		}
		return null;
	}

	public Map<String, Observer> getObservers() {
		if (observers == null)
			observers = new HashMap<String, Observer>();
		return observers;
	}

	public Observer getObserver(String observerID) {
		return this.observers.get(observerID);
	}

	public Boolean hasObserver(String ID) {
		if (getObservers().containsKey(ID))
			return true;
		return false;
	}

	public void addObserver(Observer observer) {
		observers.put(observer.getID(), (Observer) observer);
		for (IObserverListener listener : getObserverListener()) {
			listener.addedObserver(this, observer);
		}
	}

	public void removeObserver(String observerID) {
		if (hasObserver(observerID))
			observers.get(observerID).beforeDelete(this);
		observers.remove(observerID);
		for (IObserverListener listener : getObserverListener()) {
			listener.removedObserver(this);
		}
	}

	public Map<String, SchedulerEvent> getEvents() {
		if (events == null)
			events = new HashMap<String, SchedulerEvent>();
		return events;
	}

	public SchedulerEvent getEvent(String ID) {
		return events.get(ID);
	}

	public Boolean hasEvent(String ID) {
		if (getEvents().containsKey(ID))
			return true;
		return false;
	}

	public void addEvent(String eventID, SchedulerEvent schedulerEvent) {
		events.put(eventID, schedulerEvent);
	}

	public void removeEvent(String eventID) {
		if (hasEvent(eventID))
			events.get(eventID).beforeDelete(this);
		events.remove(eventID);
	}

	public Map<String, AbstractAttribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, AbstractAttribute> attributes) {
		this.attributes = attributes;
	}

	public void setParent(BControl parent) {
		this.parent = parent;
	}

	public BControl getParent() {
		return this.parent;
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		getListeners().addPropertyChangeListener(listener);
	}

	public PropertyChangeSupport getListeners() {
		if (listeners == null)
			listeners = new PropertyChangeSupport(this);
		return listeners;
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		getListeners().removePropertyChangeListener(listener);
	}

	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
		if (adapter == IPropertySource.class) {
			return new BControlPropertySource(this);
		}
		return null;
	}

	public boolean contains(BControl child) {
		return children.contains(child);
	}

	public String getDefaultValue(String attributeID) {

		IConfigurationElement configurationElement = BMotionEditorPlugin
				.getControlExtension(getType());

		if (configurationElement != null) {

			for (final IConfigurationElement configBAttributes : configurationElement
					.getChildren("attributes")) {

				for (final IConfigurationElement configBAttribute : configBAttributes
						.getChildren("attribute-string")) {

					String aID = configBAttribute.getAttribute("id");

					if (aID.equals(attributeID)) {
						String attributeDefaultValue = configBAttribute
								.getAttribute("default-value");

						return attributeDefaultValue;
					}

				}

			}

		}

		return null;

	}

	public Boolean isAttributeEditable(String attributeID) {

		IConfigurationElement configurationElement = BMotionEditorPlugin
				.getControlExtension(getType());

		if (configurationElement != null) {

			for (final IConfigurationElement configBAttributes : configurationElement
					.getChildren("attributes")) {

				for (final IConfigurationElement configBAttribute : configBAttributes
						.getChildren("attribute-string")) {

					String aID = configBAttribute.getAttribute("id");

					if (aID.equals(attributeID)) {
						String val = configBAttribute.getAttribute("editable");
						return Boolean.valueOf(val);
					}

				}

			}

		}

		return false;

	}

	public Visualization getVisualization() {
		return visualization;
	}

	public void setVisualization(Visualization visualization) {
		this.visualization = visualization;
	}

	protected void populateVisualization(Visualization visualization) {
		// Populate visualization node
		setVisualization(visualization);
		for (BControl child : getChildrenArray())
			child.populateVisualization(visualization);
		for (BConnection con : getTargetConnections())
			con.populateVisualization(visualization);
		for (BConnection con : getSourceConnections())
			con.populateVisualization(visualization);
	}

	@Override
	public BControl clone() throws CloneNotSupportedException {

		BControl clonedControl = (BControl) super.clone();

		IConfigurationElement configElement = BMotionEditorPlugin
				.getControlServices().get(getType());
		if (configElement != null) {

			try {

				IBControlService service = (IBControlService) configElement
						.createExecutableExtension("service");
				clonedControl = service.createControl(visualization);

				clonedControl.setParent(getParent());

				String newID = clonedControl.getID();

				Map<String, AbstractAttribute> newProperties = new HashMap<String, AbstractAttribute>();
				for (Entry<String, AbstractAttribute> e : getAttributes()
						.entrySet()) {
					AbstractAttribute idAtr = e.getValue().clone();
					newProperties.put(e.getKey(), idAtr);
				}

				clonedControl.setAttributes(newProperties);
				clonedControl.setAttributeValue(
						AttributeConstants.ATTRIBUTE_ID, newID);

				Iterator<BControl> it = getChildrenArray().iterator();
				while (it.hasNext()) {
					clonedControl.addChild(((BControl) it.next()).clone());
				}

				for (Observer observer : observers.values()) {
					clonedControl.addObserver(observer.clone());
				}

				for (Map.Entry<String, SchedulerEvent> e : events.entrySet()) {
					clonedControl.addEvent(e.getKey(), e.getValue().clone());
				}

			} catch (CoreException e) {
				e.printStackTrace();
			}

		}

		return clonedControl;

	}

	public void checkObserver(final Animation animation) {

		// Check all Observers
		for (Observer observer : getObservers().values()) {
			observer.check(animation, BControl.this);
		}

		// TODO: Currently connection observer are checked twice (source +
		// target) => change this, so that observer are checked only on time per
		// state!!!
		for (BConnection con : getSourceConnections()) {
			con.checkObserver(animation);
		}
		for (BConnection con : getTargetConnections()) {
			con.checkObserver(animation);
		}

		// Check Observers of children
		if (getChildrenArray().size() > 0) {
			for (BControl bcontrol : getChildrenArray()) {
				bcontrol.checkObserver(animation);
			}
		}

	}

	public void afterCheckObserver(Animation animation) {
		// Check all Observers
		for (Observer observer : getObservers().values()) {
			observer.afterCheck(animation, this);
		}
		// Check Observers of children
		if (getChildrenArray().size() > 0) {
			for (BControl bcontrol : getChildrenArray()) {
				bcontrol.afterCheckObserver(animation);
			}
		}
	}

	public void executeEvent(String eventID) {
		if (hasAttribute(AttributeConstants.ATTRIBUTE_ENABLED)) {
			if (!(Boolean) getAttributeValue(AttributeConstants.ATTRIBUTE_ENABLED))
				return;
		}
		SchedulerEvent e = getEvents().get(eventID);
		if (e != null)
			e.execute(getVisualization().getAnimation(), this);
	}

	public void setVerticalGuide(BMotionGuide verticalGuide) {
		this.verticalGuide = verticalGuide;
	}

	public BMotionGuide getVerticalGuide() {
		return verticalGuide;
	}

	public void setHorizontalGuide(BMotionGuide horizontalGuide) {
		this.horizontalGuide = horizontalGuide;
	}

	public BMotionGuide getHorizontalGuide() {
		return horizontalGuide;
	}

	/**
	 * @return the observerListener
	 */
	public ArrayList<IObserverListener> getObserverListener() {
		if (observerListener == null)
			observerListener = new ArrayList<IObserverListener>();
		return observerListener;
	}

	public void addObserverListener(IObserverListener listener) {
		getObserverListener().add(listener);
	}

	public void removeObserverListener(IObserverListener listener) {
		getObserverListener().remove(listener);
	}

	/**
	 * Return a List of outgoing Connections.
	 */
	public List<BConnection> getSourceConnections() {
		if (this.sourceConnections == null)
			this.sourceConnections = new ArrayList<BConnection>();
		return this.sourceConnections;
	}

	public void setSourceConnections(List<BConnection> connections) {
		this.sourceConnections = connections;
	}

	public void setTargetConnections(List<BConnection> connections) {
		this.targetConnections = connections;
	}

	/**
	 * Return a List of incoming Connections.
	 */
	public List<BConnection> getTargetConnections() {
		if (this.targetConnections == null)
			this.targetConnections = new ArrayList<BConnection>();
		return this.targetConnections;
	}

	public boolean hasConnections() {
		return !getTargetConnections().isEmpty()
				|| !getSourceConnections().isEmpty();
	}

	public boolean showInOutlineView() {
		return true;
	}

	public void setObserverMap(Map<String, Observer> observerMap) {
		observers = observerMap;
	}

	public void setEventMap(Map<String, SchedulerEvent> eventMap) {
		events = eventMap;
	}

	public abstract String getType();

	protected void initAttribute(AbstractAttribute atr) {
		getAttributes().put(atr.getID(), atr);
	}

	protected void initAttribute(AbstractAttribute atr, AbstractAttribute group) {
		initAttribute(atr, group.getClass().getName());
	}

	protected void initAttribute(AbstractAttribute atr, String group) {
		atr.setGroup(group);
		initAttribute(atr);
	}

	public boolean canHaveChildren() {
		return false;
	}

	public String getValueOfData() {
		return getAttributeValue(AttributeConstants.ATTRIBUTE_CUSTOM)
				.toString();
	}

}
