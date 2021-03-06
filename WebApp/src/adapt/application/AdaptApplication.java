package adapt.application;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.restlet.Application;
import org.restlet.Directory;
import org.restlet.Restlet;
import org.restlet.Router;
import org.restlet.data.LocalReference;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import adapt.entities.Action;
import adapt.entities.Resource;
import adapt.entities.User;
import adapt.entities.UserRights;
import adapt.resources.DeleteResource;
import adapt.resources.HomeResource;
import adapt.resources.IndexResource;
import adapt.resources.ModifyResource;
import adapt.resources.ProfilIzmenaResource;
import adapt.resources.ViewResource;
import adapt.resources.AddResource;
import adapt.utils.XMLAttribute;
import adapt.utils.XMLForm;
import adapt.utils.XMLManyToManyAttribute;
import adapt.utils.XMLManyToOneAttribute;
import adapt.utils.XMLOneToManyAttribute;
import adapt.utils.XMLOperation;
import adapt.utils.XMLParser;
import adapt.utils.XMLResource;
import freemarker.cache.ClassTemplateLoader;

public class AdaptApplication extends Application {

	private freemarker.template.Configuration fmc;
	private EntityManagerFactory emf;
	public ArrayList<XMLResource> XMLResources = new ArrayList<XMLResource>();
	private ArrayList<Action> actions = new ArrayList<Action>();
	private ArrayList<XMLForm> forms = new ArrayList<XMLForm>();
	private AdaptMainFrame mainFrame;

	SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy  H:mm:ss:SSS");
	Date today = new Date();

	public AdaptApplication(AdaptMainFrame mf) {
		super();
		this.mainFrame = mf;
		try {
			this.fmc = new freemarker.template.Configuration();
			this.fmc.setClassForTemplateLoading(this.getClass(), "/templates");
		} catch (Exception e) {
			getLogger().severe("Unable to configure freemarker.");
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			String stacktrace = sw.toString();
			this.mainFrame.displayText(stacktrace, 1);
		}

		forms = getForms();
		XMLResources = getXMLResources("resources");
		XMLResources.addAll(getXMLResources("resources-generated"));
		emf = Persistence.createEntityManagerFactory("adapt");
		actions = getActions();

		persistTestData();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Restlet createRoot() {
		Router router = new Router(getContext());
		File f = new File(".");
		String appPath = f.getAbsolutePath().substring(0,f.getAbsolutePath().length()-1);

		File file = new File(appPath + "src/adapt/templates");
		LocalReference lf = LocalReference.createFileReference(file);
		Directory dir = new Directory(getContext(), lf);

		Resource ress = new Resource();
		ress.setLink("/homepage");
		ress.setName("Naslovna strana");
		EntityManager ee = emf.createEntityManager();
		EntityTransaction tt = ee.getTransaction();
		tt.begin();
		ee.persist(ress);
		tt.commit();
		//		for(int i=0; i<XMLResources.size(); i++) {
		//			XMLResource r = XMLResources.get(i);
		//			if(r.getRouted()) {
		//				Resource re = new Resource();
		//				re.setName(r.getLabel());
		//				re.setLink(r.getLink());
		//				EntityManager e = emf.createEntityManager();
		//				EntityTransaction t = e.getTransaction();
		//				t.begin();
		//				e.persist(re);
		//				t.commit();
		//				e.close();
		//			}
		//		}

		router.attach("/files", dir);
		router.attach("/", IndexResource.class);
		router.attach("/homepage", HomeResource.class);
		router.attach("/profilIzmena", ProfilIzmenaResource.class);
		router.attach("/resources/{resName}", ViewResource.class);
		router.attach("/add/resources/{resName}", AddResource.class);
		router.attach("/delete/{dresName}/{delid}", DeleteResource.class); //delete page URI
		router.attach("/edit/{aresName}/{mid}/{pid}", ModifyResource.class);   //edit page URI - this goes to ModifyResource which just prepares modify form form given resouce
		router.attach("/edited/resources/{mresName}/{modid}", AddResource.class); //Add resource does the actual modifiy action
		router.attach("/showChildren/{cresName}/{presName}/{pid}", ViewResource.class);
		router.attach("/showMTMChildern/{mcresName}", ViewResource.class);
		router.attach("/mtmadded/{mtmResName}/{mtmResId}/{mtmcresName}", ViewResource.class);
		router.attach("/mtmdelete/{mtmResName}/{mtmResId}/{mtmcresName}/{mtmChId}", ViewResource.class);

		return router;
	}

	public freemarker.template.Configuration getFmc() {
		return this.fmc;
	}	

	public EntityManagerFactory getEmf(){
		return this.emf;
	}

	public ArrayList<XMLForm> getForms() {
		ArrayList<XMLForm> forms = new ArrayList<XMLForm>();
		File f = new File(".");
		String appPath = f.getAbsolutePath().substring(0,f.getAbsolutePath().length()-1);

		Document formDoc = XMLParser.parseXml(appPath + "config/forms.xml");
		NodeList frmNodes = formDoc.getElementsByTagName("form");
		for(int i=0; i<frmNodes.getLength(); i++) {
			Element element = (Element)frmNodes.item(i);
			NodeList nName = element.getElementsByTagName("name");
			NodeList nAllowActions = element.getElementsByTagName("allowActions");
			NodeList nAllowControls = element.getElementsByTagName("allowControls");
			NodeList nFilename = element.getElementsByTagName("filename");

			String name = XMLParser.getCharacterDataFromElement((Element) nName.item(0));
			Boolean actions = Boolean.parseBoolean(XMLParser.getCharacterDataFromElement((Element)nAllowActions.item(0)));
			Boolean controls = Boolean.parseBoolean(XMLParser.getCharacterDataFromElement((Element)nAllowControls.item(0)));
			String fileName = XMLParser.getCharacterDataFromElement((Element)nFilename.item(0));

			XMLForm form = new XMLForm(name, fileName, actions, controls);
			forms.add(form);
			//System.out.println("[" + formatter.format(today)  + "] Form '" + form.getName() + "' parsed");
			mainFrame.displayText("Form '" + form.getName() + "' parsed", 0);
		}
		return forms;
	}

	public ArrayList<Action> getActions() {
		ArrayList<Action> actions = new ArrayList<Action>();
		File f = new File(".");
		String appPath = f.getAbsolutePath().substring(0,f.getAbsolutePath().length()-1);

		Document resDoc = XMLParser.parseXml(appPath + "config/actions.xml");
		NodeList actNodes = resDoc.getElementsByTagName("action");
		for(int i=0; i<actNodes.getLength(); i++) {
			Element element = (Element) actNodes.item(i);
			NodeList nName = element.getElementsByTagName("name");
			NodeList nLink = element.getElementsByTagName("link");
			NodeList nIcon = element.getElementsByTagName("imagePath");
			NodeList nType = element.getElementsByTagName("type");
			NodeList nTips = element.getElementsByTagName("tip");
			NodeList nIds = element.getElementsByTagName("btnID");

			String name = XMLParser.getCharacterDataFromElement((Element) nName.item(0));
			String link = XMLParser.getCharacterDataFromElement((Element) nLink.item(0));
			String icon = XMLParser.getCharacterDataFromElement((Element) nIcon.item(0));
			String type = XMLParser.getCharacterDataFromElement((Element) nType.item(0));
			String tip  = XMLParser.getCharacterDataFromElement((Element) nTips.item(0));
			String bID  = XMLParser.getCharacterDataFromElement((Element) nIds.item(0));

			Action action = new Action();
			action.setName(name);
			action.setLink(link);
			action.setImagePath(icon);
			action.setType(type);
			action.setTip(tip);
			action.setBtnID(bID);

			actions.add(action);

			mainFrame.displayText("Action '" + action.getName() + "' parsed", 0);

			if(!action.getName().startsWith("mtm")) {
				EntityManager em = emf.createEntityManager();
				EntityTransaction tx = em.getTransaction();
				tx.begin();
				em.persist(action);
				tx.commit();
				em.close();
			}
		}
		return actions;
	}

	public ArrayList<XMLResource> getXMLResources(String file) {
		File f = new File(".");
		String appPath = f.getAbsolutePath().substring(0,f.getAbsolutePath().length()-1);
		Document resDoc = XMLParser.parseXml(appPath + "config" + File.separator + file  + ".xml");
		NodeList resNodes = resDoc.getElementsByTagName("resource");
		ArrayList<XMLResource> ress = new ArrayList<XMLResource>();

		mainFrame.displayText("Fetching resources from file: " + resDoc.getBaseURI().substring(8), 0);


		for(int i=0; i<resNodes.getLength(); i++) {
			ArrayList<XMLForm> forms = new ArrayList<XMLForm>();
			ArrayList<XMLAttribute> attributes = new ArrayList<XMLAttribute>();
			ArrayList<XMLManyToOneAttribute> manyToOneAttributes = new ArrayList<XMLManyToOneAttribute>();
			ArrayList<XMLOneToManyAttribute> oneToManyAttributes = new ArrayList<XMLOneToManyAttribute>();
			ArrayList<XMLManyToManyAttribute> manyToManyAttributes = new ArrayList<XMLManyToManyAttribute>();
			ArrayList<XMLOperation> operations = new ArrayList<XMLOperation>();

			Element element = (Element) resNodes.item(i);
			NodeList nname = element.getElementsByTagName("Name");
			NodeList nLabel = element.getElementsByTagName("Label");
			NodeList nLink = element.getElementsByTagName("Link");
			NodeList nRouted = element.getElementsByTagName("Routed");
			NodeList nForms = element.getElementsByTagName("Forms");
			NodeList nAttributes = element.getElementsByTagName("Attributes");
			NodeList nOneToMany = element.getElementsByTagName("OneToManyAttributes");
			NodeList nManyToOne = element.getElementsByTagName("ManyToOneAttributes");
			NodeList nManyToMany = element.getElementsByTagName("ManyToManyAttributes");
			NodeList nOperations = element.getElementsByTagName("Operations");

			for(int j=0; j<nAttributes.getLength(); j++) {
				Element element1 = (Element) nAttributes.item(j);
				NodeList nAttribute = element1.getElementsByTagName("attribute");
				for (int k =0; k<nAttribute.getLength(); k++) {
					Element e = (Element) nAttribute.item(k);
					NodeList nAName = e.getElementsByTagName("Name");
					NodeList nDBName = e.getElementsByTagName("DatabaseName");
					NodeList nnLabel = e.getElementsByTagName("Label");
					NodeList nType = e.getElementsByTagName("Type");
					NodeList nUnique = e.getElementsByTagName("Unique");
					NodeList nMandatory = e.getElementsByTagName("Mandatory");
					NodeList nRepresentative = e.getElementsByTagName("Representative");
					NodeList nValues = e.getElementsByTagName("Values");

					String aName = XMLParser.getCharacterDataFromElement((Element)nAName.item(0));
					String DBName = XMLParser.getCharacterDataFromElement((Element)nDBName.item(0));
					String type = XMLParser.getCharacterDataFromElement((Element)nType.item(0));
					String label = XMLParser.getCharacterDataFromElement((Element)nnLabel.item(0));
					Boolean unique = Boolean.parseBoolean(XMLParser.getCharacterDataFromElement((Element)nUnique.item(0)));
					Boolean mandatory = Boolean.parseBoolean(XMLParser.getCharacterDataFromElement((Element)nMandatory.item(0)));
					Boolean representative = Boolean.parseBoolean(XMLParser.getCharacterDataFromElement((Element)nRepresentative.item(0)));
					ArrayList<String> values = null;
					if(nValues.getLength() > 0) {
						String v = XMLParser.getCharacterDataFromElement((Element)nValues.item(0));
						System.out.println("Values za " + aName + ":" + v);
						String[] vals = v.split(";");
						values = new ArrayList<String>(Arrays.asList(vals)) ;
					}

					XMLAttribute attr = new XMLAttribute(aName, DBName, label, type, unique, mandatory, representative);
					attr.setValues(values);
					attributes.add(attr);
				}
			}

			for(int l=0; l<nOneToMany.getLength(); l++) {
				Element element2 = (Element) nOneToMany.item(l);
				NodeList nOTM = element2.getElementsByTagName("oneToMany");
				for (int m=0; m<nOTM.getLength(); m++) {
					Element el = (Element) nOTM.item(m);
					NodeList nOTMName = el.getElementsByTagName("Name");
					NodeList nOTMLabel = el.getElementsByTagName("Label");
					NodeList nRefferencedTable = el.getElementsByTagName("RefferencedTable");
					NodeList nMappedBy = el.getElementsByTagName("MappedBy");

					String OTMName = XMLParser.getCharacterDataFromElement((Element)nOTMName.item(0));
					String OTMLabel = XMLParser.getCharacterDataFromElement((Element)nOTMLabel.item(0));
					String refferencedTable = XMLParser.getCharacterDataFromElement((Element)nRefferencedTable.item(0));
					String mappedBy = XMLParser.getCharacterDataFromElement((Element)nMappedBy.item(0));

					XMLOneToManyAttribute oTMAttr = new XMLOneToManyAttribute(OTMName, OTMLabel, refferencedTable, mappedBy);
					oneToManyAttributes.add(oTMAttr);
				}
			}

			for(int n=0; n<nManyToOne.getLength(); n++) {
				Element element3 = (Element) nManyToOne.item(n);
				NodeList nMTO = element3.getElementsByTagName("manyToOne");
				for(int o=0; o<nMTO.getLength(); o++) {
					Element ele = (Element) nMTO.item(o);
					NodeList nMTOName = ele.getElementsByTagName("Name");
					NodeList nMTOLabel = ele.getElementsByTagName("Label");
					NodeList nDatabaseName = ele.getElementsByTagName("DatabaseName");
					NodeList nType = ele.getElementsByTagName("Type");
					NodeList nMandatory = ele.getElementsByTagName("Mandatory");

					String MTOName = XMLParser.getCharacterDataFromElement((Element)nMTOName.item(0));
					String databaseName = XMLParser.getCharacterDataFromElement((Element)nDatabaseName.item(0));
					String MTOLabel = XMLParser.getCharacterDataFromElement((Element)nMTOLabel.item(0));
					String type = XMLParser.getCharacterDataFromElement((Element)nType.item(0));
					Boolean mand = Boolean.parseBoolean(XMLParser.getCharacterDataFromElement((Element)nMandatory.item(0)));

					XMLManyToOneAttribute MTOAttr = new XMLManyToOneAttribute(MTOName, databaseName, MTOLabel, type, mand);
					manyToOneAttributes.add(MTOAttr);
				}
			}

			for(int m=0; m<nManyToMany.getLength(); m++) {
				Element element4 = (Element) nManyToMany.item(m);
				NodeList nMTM = element4.getElementsByTagName("manyToMany");
				for(int p=0; p<nMTM.getLength(); p++) {
					Element elle = (Element) nMTM.item(p);
					NodeList nMTMName = elle.getElementsByTagName("name");
					NodeList nMTMDBName = elle.getElementsByTagName("databaseName");
					NodeList nMTMLabel = elle.getElementsByTagName("label");
					NodeList nMTMType = elle.getElementsByTagName("type");
					NodeList nMTMJoinTable = elle.getElementsByTagName("joinTable");
					NodeList nMTMJoinColumn = elle.getElementsByTagName("joinColumns");
					NodeList nMTMInvJColumn = elle.getElementsByTagName("inverseJoinColumns");

					String MTMName = XMLParser.getCharacterDataFromElement((Element) nMTMName.item(0));
					String MTMDBName = XMLParser.getCharacterDataFromElement((Element) nMTMDBName.item(0));
					String MTMLabel = XMLParser.getCharacterDataFromElement((Element) nMTMLabel.item(0));
					String MTMType = XMLParser.getCharacterDataFromElement((Element) nMTMType.item(0));
					String MTMJoinTable = XMLParser.getCharacterDataFromElement((Element) nMTMJoinTable.item(0));
					String MTMJoinColumn = XMLParser.getCharacterDataFromElement((Element) nMTMJoinColumn.item(0));
					String MTMInvJColumn = XMLParser.getCharacterDataFromElement((Element) nMTMInvJColumn.item(0));

					XMLManyToManyAttribute MTMAttr = new XMLManyToManyAttribute(MTMName, MTMDBName, MTMLabel, MTMType, MTMJoinTable, MTMJoinColumn, MTMInvJColumn);
					manyToManyAttributes.add(MTMAttr);
				}
			}


			/*
			 * 	<operation allowed="true" label="Transaction" name="Transaction" target="null" type="transaction"/>
				<operation allowed="true" label="Report" name="Report" target="null" type="report"/>
			 */
			for(int n=0; n<nOperations.getLength(); n++) {
				Element operationElement = (Element) nOperations.item(n);
				NodeList nOps = operationElement.getElementsByTagName("operation");
				for(int s=0; s<nOps.getLength(); s++) {
					Element nOp = (Element) nOps.item(s);
					String allowed = nOp.getAttribute("allowed");
					String label = nOp.getAttribute("label");
					String name = nOp.getAttribute("name");
					String target = nOp.getAttribute("target");
					String type = nOp.getAttribute("type");

					XMLOperation op = new XMLOperation(Boolean.parseBoolean(allowed), label, name, target, type, "");
					operations.add(op);
				}
			}


			String name = XMLParser.getCharacterDataFromElement((Element) nname.item(0));
			String label = XMLParser.getCharacterDataFromElement((Element) nLabel.item(0));
			String link = XMLParser.getCharacterDataFromElement((Element)nLink.item(0));
			Boolean routed = Boolean.parseBoolean(XMLParser.getCharacterDataFromElement((Element) nRouted.item(0)));
			if(!(nForms.getLength() == 0)) {
				String sforms = XMLParser.getCharacterDataFromElement((Element) nForms.item(0));
				/*String[] fform = sforms.split(",");
	 	        for(int in=0; in<fform.length; in++) {
	 	        	XMLForm ff = getForm(fform[in]);
	 	        	forms.add(ff);
	 	        }*/
				//Ovo je bilo u starom frameworku iz mog master rada, sada vise ne treba
				XMLForm form = new XMLForm(sforms, "", true, true);
				forms.add(form);
			}

			XMLResource res = new XMLResource(name, label, link, routed, forms, attributes, manyToOneAttributes, oneToManyAttributes, manyToManyAttributes, operations);

			mainFrame.displayText("XML Resource '" + res.getName() + "' parsed", 0);
			ress.add(res);
		}
		return ress;
	}

	/**
	 * Stores one user (admin) to database and sets all permissions for that user
	 * This method is used for fast prototyping purposes only and needs to be removed once user administration module
	 * is added to mockup tool.
	 */
	public void persistTestData() {
		User admin = new User();
		admin.setUsername("admin");
		admin.setPassword("admin");

		EntityManager em = emf.createEntityManager();
		em.getTransaction().begin();

		em.persist(admin);

		for (XMLResource resource : XMLResources) {

			Resource res = new Resource();
			res.setLink(resource.getLink());
			res.setName(resource.getName());
			em.persist(res);

			for (Action action : actions) {
				UserRights right = new UserRights();
				right.setAllowed(true);
				right.setAction(action);

				right.setResource(res);
				right.setUser(admin);
				
				em.persist(right);
			}
		}

		em.getTransaction().commit();
		//em.flush();
		//em.close();
	}

	//returns list with names of all resources which reference passed resource (ManyToOne)
	public ArrayList<XMLResource> getResourceChildern(XMLResource resource) {
		ArrayList<XMLResource> childern = new ArrayList<XMLResource>();
		for(int i=0; i<XMLResources.size(); i++) {
			XMLResource res = XMLResources.get(i);
			for(int j=0; j<res.getManyToOneAttributes().size(); j++) {
				XMLManyToOneAttribute mattr = res.getManyToOneAttributes().get(j);
				if(mattr.getType().equals(resource.getName())) {
					childern.add(res);
				}
			}
		}
		return childern;
	}

	//returns list with names of all resources which reference passed resource (ManyToMany)
	public ArrayList<XMLResource> getManyToManyChildern(XMLResource resource) {
		ArrayList<XMLResource> childern = new ArrayList<XMLResource>();
		for(int i=0; i<XMLResources.size(); i++) {
			XMLResource res = XMLResources.get(i);
			for(int j=0; j<res.getManyToManyAttributes().size(); j++) {
				XMLManyToManyAttribute mtmattr = res.getManyToManyAttributes().get(j);
				if(mtmattr.getType().equals(resource.getName())) {
					childern.add(res);
				}
			}
		}
		return childern;
	}

	public XMLResource getXMLResource(String resource) {
		XMLResource res = null;
		for(int i=0; i<XMLResources.size();i++) {
			if(XMLResources.get(i).getLink().equals("/resources/" + resource)) {
				res = XMLResources.get(i);
				return res;
			}else if(XMLResources.get(i).getName().equals(resource)) {
				res = XMLResources.get(i);
				return res;
			}
		}
		return res;
	}

	public Action getAction(String name) {
		Action action = null;
		for(int i=0; i<actions.size(); i++) {
			if(actions.get(i).getName().equals(name)) {
				action = actions.get(i);
			}
		}
		return action;
	}

	public XMLForm getForm(String name) {
		XMLForm form = null;
		for(int i=0; i<forms.size(); i++) {
			if(forms.get(i).getName().equals(name)) {
				form = forms.get(i);
			}
		}
		return form;
	}

	public AdaptMainFrame getMainFrame() {
		return mainFrame;
	}

	public void setMainFrame(AdaptMainFrame mainFrame) {
		this.mainFrame = mainFrame;
	}
}
