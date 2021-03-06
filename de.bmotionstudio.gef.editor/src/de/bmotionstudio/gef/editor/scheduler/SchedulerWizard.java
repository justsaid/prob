/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package de.bmotionstudio.gef.editor.scheduler;

import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Point;

import de.bmotionstudio.gef.editor.model.BControl;

/**
 * 
 * The BMotion Studio provides an easy way to handle Scheduler Events. For this,
 * Scheduler Events can have a corresponding dialog. The user can open it by
 * calling the context menu of a B-Control.
 * 
 * @author Lukas Ladenberger
 * 
 */
public abstract class SchedulerWizard extends Wizard {

	private BControl bcontrol;
	private SchedulerEvent event;

	protected Boolean eventDelete = false;

	public SchedulerWizard(BControl bcontrol, SchedulerEvent scheduler) {
		this.bcontrol = bcontrol;
		this.event = scheduler;
	}

	public BControl getBControl() {
		return this.bcontrol;
	}

	public SchedulerEvent getScheduler() {
		return this.event;
	}

	protected abstract Boolean prepareToFinish();

	@Override
	public boolean performFinish() {
		return prepareToFinish();
	}

	protected void setEventDelete(Boolean b) {
		this.eventDelete = b;
	}

	public Boolean isEventDelete() {
		return this.eventDelete;
	}

	public abstract Point getSize();

}
