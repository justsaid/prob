package de.bmotionstudio.gef.editor.attribute;

import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;

public class BAttributeFalseValue extends AbstractAttribute {

	public BAttributeFalseValue(Object value) {
		super(value);
	}

	@Override
	public PropertyDescriptor preparePropertyDescriptor() {
		return new TextPropertyDescriptor(getID(), getName());
	}

	@Override
	public String getName() {
		return "False-Value";
	}

}
