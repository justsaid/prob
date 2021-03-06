/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package de.bmotionstudio.gef.editor.part;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPartFactory;

import de.bmotionstudio.gef.editor.model.BControl;

public class AppTreeEditPartFactory implements EditPartFactory {

	public EditPart createEditPart(EditPart context, Object model) {
		BControlTreeEditPart part = null;
		if (model instanceof BControl) {
			part = new BControlTreeEditPart();
			if (part != null)
				part.setModel(model);
		}
		return part;
	}

}
