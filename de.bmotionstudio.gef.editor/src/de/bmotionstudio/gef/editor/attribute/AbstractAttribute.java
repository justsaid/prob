/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package de.bmotionstudio.gef.editor.attribute;

import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import de.bmotionstudio.gef.editor.model.BControl;

/**
 * 
 * Represents an attribute of a {@link BControl}.
 * 
 * @author Lukas Ladenberger
 * 
 */
public abstract class AbstractAttribute implements IPropertySource, Cloneable {

	public static final String ROOT = "de.bmotionstudio.gef.editor.attribute.BAttributeRoot";

	private transient HashMap<String, AbstractAttribute> children;
	private transient BControl control;
	private String group;
	private boolean editable;
	private boolean show;
	private Object value;

	private transient PropertyDescriptor propertyDescriptor;
	private transient Object initValue;

	public AbstractAttribute(Object value) {
		this.value = value;
		this.initValue = value;
		this.editable = true;
		this.show = true;
	}

	private Object readResolve() {
		this.initValue = this.value;
		return this;
	}

	public void addChild(AbstractAttribute atr) {
		if (!getChildren().containsKey(atr.getID())) {
			getChildren().put(atr.getID(), atr);
		}
	}

	public Boolean hasChildren() {
		return !getChildren().isEmpty();
	}

	public PropertyDescriptor getPropertyDescriptor() {
		propertyDescriptor = new PropertyDescriptor(getID(), getName());
		if (editable) {
			propertyDescriptor = preparePropertyDescriptor();
			if (propertyDescriptor != null) {
				propertyDescriptor.setValidator(new ICellEditorValidator() {
					public String isValid(Object value) {
						return validateValue(value, control);
					}
				});
			}
		}
		return propertyDescriptor;
	}

	protected abstract PropertyDescriptor preparePropertyDescriptor();

	public Object unmarshal(String s) {
		return s;
	}

	public String getID() {
		return getClass().getName();
	}

	public abstract String getName();

	public void setGroup(AbstractAttribute group) {
		setGroup(group.getClass().getName());
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getGroup() {
		return group;
	}

	public Object getEditableValue() {
		return this;
	}

	public IPropertyDescriptor[] getPropertyDescriptors() {
		ArrayList<IPropertyDescriptor> descriptor = new ArrayList<IPropertyDescriptor>();
		for (AbstractAttribute atr : getChildren().values()) {
			descriptor.add(atr.getPropertyDescriptor());
		}
		return descriptor.toArray(new IPropertyDescriptor[0]);
	}

	public Object getPropertyValue(Object attrID) {
		AbstractAttribute atr = getChildren().get(attrID);
		if (atr.hasChildren()) {
			return atr;
		} else {
			return atr.getValue();
		}
	}

	public boolean isPropertySet(Object id) {
		return false;
	}

	public void resetPropertyValue(Object id) {
	}

	public void setPropertyValue(Object id, Object value) {
		AbstractAttribute atr = children.get(id);
		atr.setValue(value);
	}

	public void setValue(Object value) {
		setValue(value, true);
	}

	public void setValue(Object value, Boolean firePropertyChange) {
		Object oldVal = this.value;
		this.value = value;
		if (firePropertyChange && control != null)
			control.getListeners().firePropertyChange(getID(), oldVal, value);
	}

	public void restoreValue() {
		Object oldVal = this.value;
		this.value = this.initValue;
		if (control != null)
			control.getListeners().firePropertyChange(getID(), oldVal, value);
	}

	public Object getValue() {
		return this.value;
	}

	public Object getInitValue() {
		return initValue;
	}

	public HashMap<String, AbstractAttribute> getChildren() {
		if (children == null)
			children = new HashMap<String, AbstractAttribute>();
		return children;
	}

	@Override
	public AbstractAttribute clone() throws CloneNotSupportedException {
		return (AbstractAttribute) super.clone();
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}

	public boolean isEditable() {
		return editable;
	}

	public String validateValue(Object value, BControl control) {
		return null;
	}

	public void setShow(boolean show) {
		this.show = show;
	}

	public boolean show() {
		return show;
	}

	public BControl getControl() {
		return control;
	}

	public void setControl(BControl control) {
		this.control = control;
	}

}
