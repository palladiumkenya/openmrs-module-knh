package org.openmrs.module.knh.metadata;

import org.openmrs.module.metadatadeploy.bundle.AbstractMetadataBundle;
import org.springframework.stereotype.Component;

import static org.openmrs.module.metadatadeploy.bundle.CoreConstructors.*;

/**
 * Implementation of access control to the app.
 */
@Component
public class KnhAdminSecurityMetadata extends AbstractMetadataBundle {
	
	public static class _Privilege {
		
		public static final String APP_KNH_ADMIN = "App: knh.appointments";
	}
	
	public static final class _Role {
		
		public static final String APPLICATION_KNH_ADMIN = "KNH Administration";
		
		public static final String API_PRIVILEGES_VIEW_AND_EDIT = "API Privileges (View and Edit)";
	}
	
	/**
	 * @see AbstractMetadataBundle#install()
	 */
	@Override
	public void install() {
		
		install(privilege(_Privilege.APP_KNH_ADMIN, "Able to access Appointments Rescheduling"));
		install(role(_Role.APPLICATION_KNH_ADMIN, "Can access Appointments app", idSet(_Role.API_PRIVILEGES_VIEW_AND_EDIT),
		    idSet(_Privilege.APP_KNH_ADMIN)));
	}
}
