package com.panelcomposer.elements;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import net.miginfocom.swing.MigLayout;
import util.staticnames.Messages;

import com.panelcomposer.elements.spanel.SPanel;
import com.panelcomposer.enumerations.OpenedAs;
import com.panelcomposer.model.panel.MManyToManyPanel;
import com.panelcomposer.model.panel.MPanel;
import com.panelcomposer.model.panel.MParentChildPanel;
import com.panelcomposer.model.panel.MStandardPanel;

/****
 * Modal dialog that represents standard form which contains the standard panels
 * created dynamic of their models
 * 
 * @author Darko
 * 
 */
@SuppressWarnings("serial")
public class SForm extends JDialog {

	/***
	 * Model describing the panel
	 */
	protected MPanel mpanel;
	/***
	 * Panels that are on added to the form.
	 */
	protected List<SPanel> panels = new ArrayList<SPanel>();;

	/***
	 * SForm constructor when panel's model is of MStandardPanel instance.
	 * 
	 * @param mpanel
	 *            Standard panel's model.
	 * @param ownerPanel
	 *            Panel that invoked viewing of this form (it's form will be
	 *            parent for this one)
	 * @param openedAs
	 *            Mode in which panel is set to be viewed
	 * @param dataFilter
	 *            SQL "where" clausule
	 * 
	 */
	public SForm(MStandardPanel mpanel, SPanel ownerPanel, OpenedAs openedAs,
			String dataFilter) {
		setModal(true);
		this.mpanel = mpanel;
		init();
	}

	/***
	 * SForm constructor when panel's model is of MParentChildPanel instance.
	 * 
	 * @param mpanel
	 *            Parent-child panel's model.
	 * @param ownerPanel
	 *            Panel that invoked viewing of this form (it's form will be
	 *            parent for this one)
	 */
	public SForm(MParentChildPanel mpanel, SPanel ownerPanel) {
		setModal(true);
		this.mpanel = mpanel;
		init();
	}

	/***
	 * SForm constructor when panel's model is of MManyToManyPanel instance.
	 * 
	 * @param mpanel
	 *            Many-to-Many Panel's model.
	 * @param ownerPanel
	 *            Panel that invoked viewing of this form (it's form will be
	 *            parent for this one)
	 */
	public SForm(MManyToManyPanel mpanel, SPanel ownerPanel) {
		setModal(true);
		this.mpanel = mpanel;
		init();
	}

	/****
	 * Initialization method for setting up grounds for standard form: look and
	 * feel, layout manager, scroll pane.
	 */
	private void init() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, Messages.LOOK_AND_FEEL,
					Messages.ERROR, JOptionPane.INFORMATION_MESSAGE);
		}
		
		MigLayout layout = new MigLayout("fill");
		setLayout(layout);
		this.setLocationRelativeTo(null);
		//setResizable(false);
	}

	public void addToPane(JComponent component) {
		add(component, "grow");
	}

	public MPanel getMpanel() {
		return mpanel;
	}

	public void setMpanel(MPanel mpanel) {
		this.mpanel = mpanel;
	}

	public List<SPanel> getPanels() {
		return panels;
	}

	public void setPanels(List<SPanel> panels) {
		this.panels = panels;
	}

}
