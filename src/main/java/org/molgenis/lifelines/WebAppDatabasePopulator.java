package org.molgenis.lifelines;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.molgenis.MolgenisDatabasePopulator;
import org.molgenis.catalogmanager.CatalogManagerController;
import org.molgenis.dataexplorer.controller.DataExplorerController;
import org.molgenis.framework.db.Database;
import org.molgenis.framework.db.DatabaseException;
import org.molgenis.framework.db.QueryRule;
import org.molgenis.framework.db.QueryRule.Operator;
import org.molgenis.framework.security.Login;
import org.molgenis.framework.server.MolgenisPermissionService;
import org.molgenis.framework.server.MolgenisPermissionService.Permission;
import org.molgenis.lifelines.controller.HomeController;
import org.molgenis.omx.auth.MolgenisGroup;
import org.molgenis.omx.auth.MolgenisRole;
import org.molgenis.omx.auth.MolgenisRoleGroupLink;
import org.molgenis.omx.auth.MolgenisUser;
import org.molgenis.omx.auth.OmxPermissionService;
import org.molgenis.omx.auth.controller.AccountController;
import org.molgenis.omx.auth.controller.UserAccountController;
import org.molgenis.omx.auth.service.AccountService;
import org.molgenis.omx.core.RuntimeProperty;
import org.molgenis.omx.observ.Category;
import org.molgenis.omx.observ.Characteristic;
import org.molgenis.omx.observ.DataSet;
import org.molgenis.omx.observ.ObservableFeature;
import org.molgenis.omx.observ.ObservationSet;
import org.molgenis.omx.observ.ObservedValue;
import org.molgenis.omx.observ.Protocol;
import org.molgenis.omx.observ.target.Ontology;
import org.molgenis.omx.observ.target.OntologyTerm;
import org.molgenis.omx.order.OrderStudyDataController;
import org.molgenis.omx.protocolviewer.ProtocolViewerController;
import org.molgenis.omx.study.StudyDataRequest;
import org.molgenis.search.SearchSecurityHandlerInterceptor;
import org.molgenis.studymanager.StudyManagerController;
import org.molgenis.ui.MolgenisMenuController.VoidPluginController;
import org.molgenis.util.Entity;
import org.springframework.beans.factory.annotation.Value;

public class WebAppDatabasePopulator extends MolgenisDatabasePopulator
{
	static final String KEY_APP_NAME = "app.name";
	static final String KEY_APP_HREF_LOGO = "app.href.logo";
	static final String KEY_APP_HREF_CSS = "app.href.css";

	@Value("${lifelines.profile:@null}")
	private String appProfileStr;
	@Value("${admin.password:@null}")
	private String adminPassword;
	@Value("${lifelines.datamanager.password:@null}")
	private String dataManagerPassword;
	@Value("${lifelines.datamanager.email:molgenis+datamanager@gmail.com}")
	private String dataManagerEmail;
	@Value("${lifelines.researcher.password:@null}")
	private String researcherPassword;
	@Value("${lifelines.researcher.email:molgenis+researcher@gmail.com}")
	private String researcherEmail;

	@Override
	protected void initializeApplicationDatabase(Database database) throws Exception
	{
		if (appProfileStr == null || dataManagerPassword == null || researcherPassword == null || adminPassword == null)
		{
			StringBuilder message = new StringBuilder("please configure: ");
			if (appProfileStr == null) message.append("lifelines.profile(possible values: workspace or website), ");
			if (dataManagerPassword == null) message.append("default lifelines.datamanager.password, ");
			if (researcherPassword == null) message.append("default lifelines.researcher.password ");
			if (adminPassword == null) message.append("default admin.password ");
			message.append("in your molgenis-server.properties.");
			throw new RuntimeException(message.toString());
		}

		String homeHtml = "<div class=\"container-fluid\">"
				+ "<div class=\"row-fluid\">"
				+ "<div class=\"span6\">"
				+ "<h3>Welcome at the LifeLines Data Catalogue!</h3>"
				+ "<p>The LifeLines Data Catalogue provides an overview of all the data collected in LifeLines.</p>"
				+ "<p>When you click 'catalogue' you can browse all available data items from questionnaires,  measurements and (blood and urine) sample analysis. Also, you can make a selection of data  items that you will need for your research, and download the list.</p>"
				+ "<p>If you want to save your selection and apply for LifeLines data, you need to  register first. You can register by clicking the 'login' button on top. After you  have registered, you will receive a confirmation email. Subsequently, you are able  to download your selection or submit the selection together with you proposal.</p>"
				+ "<p>The catalogue will regularly be updated with new collected data items.  For questions regarding the catalogue or submission of your proposal, please contact the  LifeLines Research Office  <a href=\"mailto:LLscience@umcg.nl\">LLscience@umcg.nl</a></p>"
				+ "<p>The catalogue is working in the newest browsers. <u>If you are experiencing any problems  please switch to a modern browser (IE9+, Chrome, Firefox, Safari).</u></p>"
				+ "</div>" + "<div class=\"span6\">"
				+ "<img src=\"/img/lifelines_family.png\" alt=\"LifeLines family\">" + "</div>" + "</div>" + "</div>";

		Map<String, String> runtimePropertyMap = new HashMap<String, String>();
		runtimePropertyMap.put(KEY_APP_NAME, "LifeLines");
		runtimePropertyMap.put(KEY_APP_HREF_LOGO, "/img/lifelines_letterbox_270x100.png");
		runtimePropertyMap.put(KEY_APP_HREF_CSS, "lifelines.css");
		runtimePropertyMap.put(AccountService.KEY_PLUGIN_AUTH_ACTIVATIONMODE, "user");
		runtimePropertyMap.put(HomeController.KEY_APP_HOME_HTML, homeHtml);

		LifeLinesAppProfile appProfile = LifeLinesAppProfile.valueOf(appProfileStr.toUpperCase());

		Login login = database.getLogin();
		database.setLogin(null);
		login.login(database, Login.USER_ADMIN_NAME, adminPassword);

		MolgenisPermissionService permissionService = new OmxPermissionService(database, login);

		for (Entry<String, String> entry : runtimePropertyMap.entrySet())
		{
			RuntimeProperty runtimeProperty = new RuntimeProperty();
			String propertyKey = entry.getKey();
			runtimeProperty.setIdentifier(RuntimeProperty.class.getSimpleName() + '_' + propertyKey);
			runtimeProperty.setName(propertyKey);
			runtimeProperty.setValue(entry.getValue());
			database.add(runtimeProperty);
		}

		MolgenisUser userResearcher = createUser(database, "researcher", "researcher", "researcher", researcherEmail,
				researcherPassword, false);
		MolgenisUser userDataManager = createUser(database, "datamanager", "datamanager", "datamanager",
				dataManagerEmail, dataManagerPassword, false);

		MolgenisGroup groupDataManagers = createGroup(database, "dataManagers");
		MolgenisGroup groupResearchers = createGroup(database, "researchers");

		MolgenisRoleGroupLink linkDatamanager = new MolgenisRoleGroupLink();
		linkDatamanager.setGroup(groupDataManagers);
		linkDatamanager.setRole(userDataManager);
		linkDatamanager.setIdentifier(UUID.randomUUID().toString());
		linkDatamanager.setName(UUID.randomUUID().toString());
		database.add(linkDatamanager);

		MolgenisRoleGroupLink linkResearcher = new MolgenisRoleGroupLink();
		linkResearcher.setGroup(groupResearchers);
		linkResearcher.setRole(userResearcher);
		linkResearcher.setIdentifier(UUID.randomUUID().toString());
		linkResearcher.setName(UUID.randomUUID().toString());
		database.add(linkResearcher);

		MolgenisGroup allUsersGroup = null;
		List<MolgenisUser> users = database.find(MolgenisUser.class, new QueryRule(MolgenisUser.NAME, Operator.EQUALS,
				Login.USER_ANONYMOUS_NAME));
		if (users != null && !users.isEmpty())
		{
			MolgenisUser userAnonymous = users.get(0);
			List<MolgenisGroup> molgenisGroups = database.find(MolgenisGroup.class, new QueryRule(MolgenisGroup.NAME,
					Operator.EQUALS, Login.GROUP_USERS_NAME));
			if (molgenisGroups == null || molgenisGroups.isEmpty()) throw new DatabaseException(
					"missing required MolgenisGroup with name '" + Login.GROUP_USERS_NAME + "'");
			allUsersGroup = molgenisGroups.get(0);

			List<MolgenisRole> molgenisRoles = new ArrayList<MolgenisRole>();
			molgenisRoles.add(userAnonymous);
			molgenisRoles.add(userResearcher);
			molgenisRoles.add(allUsersGroup);

			List<Class<? extends Entity>> visibleClasses = new ArrayList<Class<? extends Entity>>();
			// add entity dependencies for protocol viewer plugin
			visibleClasses.add(DataSet.class);
			visibleClasses.add(Protocol.class);
			visibleClasses.add(ObservationSet.class);
			visibleClasses.add(ObservableFeature.class);
			visibleClasses.add(Category.class);
			visibleClasses.add(ObservedValue.class);

			for (Class<? extends Entity> entityClass : visibleClasses)
			{
				for (MolgenisRole molgenisRole : molgenisRoles)
				{
					permissionService.setPermissionOnEntity(entityClass, molgenisRole.getId(), Permission.READ);
				}
			}
			permissionService.setPermissionOnEntity(StudyDataRequest.class, allUsersGroup.getId(), Permission.OWN);
		}

		permissionService.setPermissionOnEntity(Characteristic.class, groupDataManagers.getId(), Permission.WRITE);
		permissionService.setPermissionOnEntity(OntologyTerm.class, groupDataManagers.getId(), Permission.WRITE);
		permissionService.setPermissionOnEntity(Ontology.class, groupDataManagers.getId(), Permission.WRITE);
		permissionService.setPermissionOnEntity(DataSet.class, groupDataManagers.getId(), Permission.WRITE);
		permissionService.setPermissionOnEntity(Protocol.class, groupDataManagers.getId(), Permission.WRITE);
		permissionService.setPermissionOnEntity(ObservationSet.class, groupDataManagers.getId(), Permission.WRITE);
		permissionService.setPermissionOnEntity(ObservableFeature.class, groupDataManagers.getId(), Permission.WRITE);
		permissionService.setPermissionOnEntity(Category.class, groupDataManagers.getId(), Permission.WRITE);
		permissionService.setPermissionOnEntity(ObservedValue.class, groupDataManagers.getId(), Permission.WRITE);
		permissionService.setPermissionOnEntity(MolgenisUser.class, groupDataManagers.getId(), Permission.WRITE);

		permissionService.setPermissionOnEntity(MolgenisUser.class, groupResearchers.getId(), Permission.WRITE);
		permissionService.setPermissionOnEntity(MolgenisUser.class, allUsersGroup.getId(), Permission.WRITE);

		MolgenisUser anonymousUser = MolgenisUser.findByName(database, Login.USER_ANONYMOUS_NAME);

		permissionService.setPermissionOnPlugin(VoidPluginController.class, allUsersGroup.getId(), Permission.READ);
		permissionService.setPermissionOnPlugin(VoidPluginController.class, groupResearchers.getId(), Permission.READ);
		permissionService.setPermissionOnPlugin(VoidPluginController.class, anonymousUser.getId(), Permission.READ);
		permissionService.setPermissionOnPlugin(HomeController.class, allUsersGroup.getId(), Permission.READ);
		permissionService.setPermissionOnPlugin(HomeController.class, groupResearchers.getId(), Permission.READ);
		permissionService.setPermissionOnPlugin(HomeController.class, anonymousUser.getId(), Permission.READ);
		permissionService.setPermissionOnPlugin(ProtocolViewerController.class, allUsersGroup.getId(), Permission.READ);
		permissionService.setPermissionOnPlugin(ProtocolViewerController.class, groupResearchers.getId(),
				Permission.READ);
		permissionService.setPermissionOnPlugin(ProtocolViewerController.class, anonymousUser.getId(), Permission.READ);
		permissionService.setPermissionOnPlugin(UserAccountController.class, allUsersGroup.getId(), Permission.READ);
		permissionService.setPermissionOnPlugin(UserAccountController.class, groupResearchers.getId(), Permission.READ);
		
		
		
		if (appProfile == LifeLinesAppProfile.WEBSITE)
		{
			permissionService.setPermissionOnPlugin(CatalogManagerController.class.getSimpleName(),
					userDataManager.getId(), Permission.READ);

			permissionService.setPermissionOnPlugin(OrderStudyDataController.class, allUsersGroup.getId(),
					Permission.WRITE);
			permissionService.setPermissionOnPlugin(OrderStudyDataController.class, groupResearchers.getId(),
					Permission.WRITE);

			RuntimeProperty runtimePropertyAllowAnonymousSearch = new RuntimeProperty();
			runtimePropertyAllowAnonymousSearch.setIdentifier(RuntimeProperty.class.getSimpleName() + '_'
					+ SearchSecurityHandlerInterceptor.KEY_ACTION_ALLOW_ANONYMOUS_SEARCH);
			runtimePropertyAllowAnonymousSearch
					.setName(SearchSecurityHandlerInterceptor.KEY_ACTION_ALLOW_ANONYMOUS_SEARCH);
			runtimePropertyAllowAnonymousSearch.setValue("true");
			database.add(runtimePropertyAllowAnonymousSearch);
		}
		else if (appProfile == LifeLinesAppProfile.WORKSPACE)
		{
			permissionService.setPermissionOnPlugin(DataExplorerController.class.getSimpleName(),
					groupDataManagers.getId(), Permission.READ);
			permissionService.setPermissionOnPlugin(DataExplorerController.class.getSimpleName(),
					groupResearchers.getId(), Permission.READ);
			permissionService.setPermissionOnPlugin(StudyManagerController.class.getSimpleName(),
					groupDataManagers.getId(), Permission.READ);
		}
		else
		{
			throw new RuntimeException(
					"please configure app.profile in your molgenis-server.properties. possible values: workspace or website");
		}

		database.setLogin(login); // restore login
	}

}