/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.packageview;


import org.eclipse.jface.util.Assert;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.ListenerList;
import org.eclipse.jface.util.PropertyChangeEvent;
import java.util.*;


/**
 * Supports a web-browser style of navigation, by maintaining a list
 * of frames.  Each frame holds a snapshot of a view at some point 
 * in time.
 * <p>
 * The frame list obtains a snapshot of the current frame from a frame source
 * on creation, and whenever switching to a different frame.
 * </p>
 * <p>
 * A property change notification is sent whenever the current page changes.
 * </p>
 */
/*
 * COPIED unmodified from org.eclipse.ui.views.navigator
 */
 
/* package */ class FrameList {
	/** Property name constant for the current frame. */
	public static final String P_CURRENT_FRAME = "currentFrame";

	private IFrameSource source;
	private List frames;
	private int current;
	private ListenerList propertyListeners = new ListenerList();
/**
 * Creates a new frame list with the given source.
 *
 * @param source the frame source
 */
public FrameList(IFrameSource source) {
	this.source = source;
	Frame frame = source.getCurrentFrame();
	frame.setParent(this);
	frame.setIndex(0);
	frames = new ArrayList();
	frames.add(frame);
	current = 0;
}
/**
 * Adds a property change listener.
 * Has no effect if an identical listener is already registered.
 *
 * @param listener a property change listener
 */
public void addPropertyChangeListener(IPropertyChangeListener listener) { 
	propertyListeners.add(listener);
}
/**
 * Moves the frame pointer back by one.
 * Has no effect if there is no frame before the current one.
 * Fires a <code>P_CURRENT_FRAME</code> property change event.
 */
public void back() {
	if (current > 0) {
		setCurrent(current-1);
	}
}
/**
 * Notifies any property change listeners that a property has changed.
 * Only listeners registered at the time this method is called are notified.
 *
 * @param event the property change event
 *
 * @see IPropertyChangeListener#propertyChange
 */
protected void firePropertyChange(PropertyChangeEvent event) {
	Object[] listeners = propertyListeners.getListeners();
	for (int i = 0; i < listeners.length; ++i) {
		((IPropertyChangeListener) listeners[i]).propertyChange(event);
	}
}
/**
 * Moves the frame pointer forward by one.
 * Has no effect if there is no frame after the current one.
 * Fires a <code>P_CURRENT_FRAME</code> property change event.
 */
public void forward() {
	if (current < frames.size()-1) {
		setCurrent(current+1);
	}
}
/**
 * Returns the current frame.
 * Returns <code>null</code> if there is no current frame.
 *
 * @return the current frame, or <code>null</code>
 */
public Frame getCurrentFrame() {
	return getFrame(current);
}
/**
 * Returns the index of the current frame.
 *
 * @return the index of the current frame
 */
public int getCurrentIndex() {
	return current;
}
/**
 * Returns the frame at the given index, or <code>null</code>
 * if the index is &le; 0 or &ge; <code>size()</code>.
 *
 * @param index the index of the requested frame
 * @return the frame at the given index or <code>null</code>
 */
public Frame getFrame(int index) {
	if (index < 0 || index >= frames.size())
		return null;
	return (Frame) frames.get(index);
}
/**
 * Returns the frame source.
 */
public IFrameSource getSource() {
	return source;
}
/**
 * Adds the given frame after the current frame,
 * and advances the pointer to the new frame.
 * Before doing so, updates the current frame, and removes any frames following the current frame.
 * Fires a <code>P_CURRENT_FRAME</code> property change event.
 *
 * @param frame the frame to add
 */
public void gotoFrame(Frame frame) {
	for (int i = frames.size(); --i > current;) {
		frames.remove(i);
	}
	frame.setParent(this);
	int index = frames.size();
	frame.setIndex(index);
	frames.add(frame);
	setCurrent(index);
}
/**
 * Removes a property change listener.
 * Has no effect if an identical listener is not registered.
 *
 * @param listener a property change listener
 */
public void removePropertyChangeListener(IPropertyChangeListener listener) {
	propertyListeners.remove(listener);
}
/**
 * Sets the current frame to the one with the given index.
 * Updates the old current frame, and fires a <code>P_CURRENT_FRAME</code> property change event
 * if the current frame changes.
 *
 * @param newCurrent the index of the frame
 */
void setCurrent(int newCurrent) {
	Assert.isTrue(newCurrent >= 0 && newCurrent < frames.size());
	int oldCurrent = this.current;
	if (oldCurrent != newCurrent) {
		updateCurrentFrame();
		this.current = newCurrent;
		firePropertyChange(new PropertyChangeEvent(this, P_CURRENT_FRAME, getFrame(oldCurrent), getFrame(newCurrent)));
	}
}
/**
 * Sets the current frame to the frame with the given index.
 * Fires a <code>P_CURRENT_FRAME</code> property change event
 * if the current frame changes.
 */
public void setCurrentIndex(int index) {
	if (index != -1 && index != current)
		setCurrent(index);
}
/**
 * Returns the number of frames in the frame list.
 */
public int size() {
	return frames.size();
}
/**
 * Replaces the current frame in this list with the current frame 
 * from the frame source.  No event is fired.
 */
public void updateCurrentFrame() {
	Assert.isTrue(current >= 0);
	Frame frame = source.getCurrentFrame();
	frame.setParent(this);
	frame.setIndex(current);
	frames.set(current, frame);
}
}
