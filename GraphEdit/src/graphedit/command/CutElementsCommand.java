package graphedit.command;

import graphedit.app.ApplicationMode;
import graphedit.app.MainFrame;
import graphedit.model.components.Connector;
import graphedit.model.components.GraphElement;
import graphedit.model.components.Link;
import graphedit.model.components.LinkableElement;
import graphedit.model.components.Package;
import graphedit.model.components.shortcuts.Shortcut;
import graphedit.model.diagram.GraphEditModel;
import graphedit.model.elements.ClassElement;
import graphedit.model.elements.GraphEditElement;
import graphedit.model.elements.GraphEditPackage;
import graphedit.model.properties.PropertyEnums.GraphElementProperties;
import graphedit.model.properties.PropertyEnums.LinkNodeProperties;
import graphedit.util.Calculate;
import graphedit.view.ElementPainter;
import graphedit.view.GraphEditView;
import graphedit.view.LinkPainter;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import kroki.uml_core_basic.UmlPackage;

public class CutElementsCommand extends Command {

	private  List<GraphElement> elements;
	private  List<ElementPainter> elementPainters;
	private  List<Link> links;
	private  List<LinkPainter> linkPainters;
	private Map<Connector, GraphElement> removedMappings;
	private GraphEditModel model;
	private List<GraphEditPackage> packages;
	private Command deletePackages;
	private UmlPackage umlPackage;

	private Map<Shortcut, GraphElement> replacements = new HashMap<Shortcut, GraphElement>();
	private List<Shortcut> unlinked = new ArrayList<Shortcut>();
	private Map<Connector, Point2D> oldPositions = new HashMap<Connector, Point2D>();

	private List<CutLinkCommand> cutLinkCommands;


	/*
	 * WIP - nakon UNDO i REDO srediti
	 */
	public CutElementsCommand(GraphEditView view, List<GraphElement> elements, List<ElementPainter> elementPainters) {
		packages = new ArrayList<GraphEditPackage>();
		this.elementPainters = new ArrayList<ElementPainter>();
		this.elements = new ArrayList<GraphElement>();
		cutLinkCommands = new ArrayList<CutLinkCommand>();

		for (GraphElement element : elements){
			if (element instanceof Package)
				packages.add( ((Package)element).getHierarchyPackage());
			else{
				this.elements.add(element);
				this.elementPainters.add(view.getElementPainter(element));
			}
		}
		this.model = view.getModel();
		this.view = view;
		this.elements = new ArrayList<GraphElement>(elements);
		this.elementPainters = new ArrayList<ElementPainter>(elementPainters);
		this.links = new ArrayList<Link>();

		deletePackages = new DeletePackagesCommand(packages, view);
		umlPackage = model.getParentPackage().getUmlPackage();

		for (GraphElement el : elements){
			if (el instanceof Shortcut){
				Shortcut s = (Shortcut) el;
				if (unlinkAfterDeletingShortcut(s))
					unlinked.add(s);
				else
					replacements.put(s, getReplacementElement(s));
			}
		}
		
		
		if (MainFrame.getInstance().getAppMode() == ApplicationMode.USER_INTERFACE && links!=null)
			for (Link link : model.getAssociatedLinks(elements)){

				boolean unlink = true;
				GraphElement source = model.getElementByConnector().get(link.getSourceConnector());
				GraphElement destination = model.getElementByConnector().get(link.getDestinationConnector());

				
				//brisemo ako se brise element 
				if (!(source instanceof Shortcut) && elements.contains(source) ||
						!(destination instanceof Shortcut) && elements.contains(destination))
						unlink = true;
				else if (source instanceof Shortcut || destination instanceof Shortcut)
					unlink = unlinked.contains(source) || unlinked.contains(destination)
					|| (elements.contains(source) && elements.contains(destination));

				
				if (unlink){
					cutLinkCommands.add(new CutLinkCommand(view, link));
				}
				else{
					links.add(link);
					
				}
			}
		this.linkPainters = view.getLinkPainters(links);


	}


	@Override
	public void execute() {
		model.removeDiagramElements(elements);
		view.removeElementPainters(elementPainters);
		view.getSelectionModel().removeAllSelectedElements();
		deletePackages.execute();
		
		removedMappings = model.removeFromElementByConnectorStructure(links);


		
		for (Connector conn : removedMappings.keySet()){
			LinkableElement element = (LinkableElement) removedMappings.get(conn);
			element.getConnectors().add(conn);
		}
		model.addToElementByConnectorStructure(removedMappings);

		for (Link link : links){

			GraphElement source = removedMappings.get(link.getSourceConnector());
			GraphElement destination = removedMappings.get(link.getDestinationConnector());


			if (source instanceof Shortcut){
				LinkableElement replacement = (LinkableElement) replacements.get(source);
				replacement.addConnectors(link.getSourceConnector());
				Point2D oldPosition = new Point2D.Double(((Point2D)link.getSourceConnector().getProperty(LinkNodeProperties.POSITION)).getX(),
						((Point2D)link.getSourceConnector().getProperty(LinkNodeProperties.POSITION)).getY());

				oldPositions.put(link.getSourceConnector(), oldPosition);
				Point2D newPosition = new Point2D.Double(((Point2D)replacement.getProperty(GraphElementProperties.POSITION)).getX(),
						((Point2D)replacement.getProperty(GraphElementProperties.POSITION)).getY());
				link.getSourceConnector().setProperty(LinkNodeProperties.POSITION, newPosition);
			
				
				model.insertIntoElementByConnectorStructure(link.getSourceConnector(), replacement);
				link.getSourceConnector().setRelativePositions(newPosition);
				link.getSourceConnector().setPercents(newPosition);
			}

			if (destination instanceof Shortcut){
				LinkableElement replacement = (LinkableElement) replacements.get(destination);
				replacement.addConnectors(link.getDestinationConnector());
				Point2D oldPosition = new Point2D.Double(((Point2D)link.getDestinationConnector().getProperty(LinkNodeProperties.POSITION)).getX(),
						((Point2D)link.getDestinationConnector().getProperty(LinkNodeProperties.POSITION)).getY());

				oldPositions.put(link.getDestinationConnector(), oldPosition);
				
				Point2D newPosition = new Point2D.Double(((Point2D)replacement.getProperty(GraphElementProperties.POSITION)).getX(),
						((Point2D)replacement.getProperty(GraphElementProperties.POSITION)).getY());
				link.getDestinationConnector().setProperty(LinkNodeProperties.POSITION, newPosition);
				
				
				model.insertIntoElementByConnectorStructure(link.getDestinationConnector(), replacement);
				link.getDestinationConnector().setRelativePositions(newPosition);
				link.getDestinationConnector().setPercents(newPosition);
			}
		}
		for (CutLinkCommand cutLink : cutLinkCommands)
			cutLink.execute();

		for (GraphElement element : elements){
			GraphElement nonShortcut = element;
			if (element instanceof Shortcut)
				nonShortcut = ((Shortcut)element).shortcutTo();

			GraphEditElement gElement = nonShortcut.getRepresentedElement();
			umlPackage.removeOwnedType(((ClassElement)gElement).getUmlType());

		}
	}

	@Override
	public void undo() {
		model.addDiagramElements(elements);
		model.addLinks(links);

		view.addElementPainters(elementPainters);
		model.addToElementByConnectorStructure(removedMappings);
		view.getSelectionModel().addSelectedElements(elements);
		deletePackages.undo();


		for (Link link : links){
			
			GraphElement source = removedMappings.get(link.getSourceConnector());
			GraphElement destination = removedMappings.get(link.getDestinationConnector());

			if (source instanceof Shortcut){

				//nije unlinkovano, samo je premesteno
				LinkableElement replacement = (LinkableElement) replacements.get(source);
				replacement.removeConnectors(link.getSourceConnector());
				((LinkableElement)source).addConnectors(link.getSourceConnector());
				link.getSourceConnector().setProperty(LinkNodeProperties.POSITION, oldPositions.get(link.getSourceConnector()));
				model.insertIntoElementByConnectorStructure(link.getSourceConnector(), source);
			}

			if (destination instanceof Shortcut){


				//nije unlinkovano, samo je premesteno
				LinkableElement replacement = (LinkableElement) replacements.get(destination);
				replacement.removeConnectors(link.getDestinationConnector());
				((LinkableElement)destination).addConnectors(link.getDestinationConnector());
				link.getDestinationConnector().setProperty(LinkNodeProperties.POSITION, oldPositions.get(link.getDestinationConnector()));
				model.insertIntoElementByConnectorStructure(link.getDestinationConnector(), source);
			}
		}

		for (CutLinkCommand cutLink : cutLinkCommands)
			cutLink.undo();

		for (GraphElement element : elements){
			GraphElement nonShortcut = element;
			if (element instanceof Shortcut)
				nonShortcut = ((Shortcut)element).shortcutTo();

			GraphEditElement gElement = nonShortcut.getRepresentedElement();
			umlPackage.addOwnedType(((ClassElement)gElement).getUmlType());

		}
	}




	private boolean unlinkAfterDeletingShortcut(Shortcut shortcut){
		//pogledaj da li ima jos neki shortcut u tom paketu/view-u
		LinkableElement element = (LinkableElement) shortcut.shortcutTo();
		for (Shortcut s : element.getShortcuts()){
			if (s == shortcut)
				continue;
			if (s.shortcutToModel() == shortcut.shortcutToModel())
				return false;
		}
		return !shortcut.shortcutToModel().getDiagramElements().contains(element);
	}

	private GraphElement getReplacementElement(Shortcut shortcut){

		LinkableElement element = (LinkableElement) shortcut.shortcutTo();
		double minDiff = -1;
		GraphElement replacement = null;
		Point2D shortcutLocation = (Point2D) ((GraphElement) shortcut).getProperty(GraphElementProperties.POSITION);
		for (Shortcut s : element.getShortcuts()){
			if (s == shortcut)
				continue;
			if (s.shortcutToModel() == shortcut.shortcutToModel()){
				double diff = Calculate.positionDiff( (Point2D) ((GraphElement)s).getProperty(GraphElementProperties.POSITION), shortcutLocation);
				if (minDiff == -1 || diff < minDiff){
					minDiff = diff;
					replacement = (GraphElement)s;
				}
			}
		}

		if (shortcut.shortcutToModel().getDiagramElements().contains(element)){
			double diff = Calculate.positionDiff( (Point2D) element.getProperty(GraphElementProperties.POSITION), shortcutLocation);
			if (minDiff == -1 || diff < minDiff){
				replacement = element;
			}

		}
		return replacement;
	}


}