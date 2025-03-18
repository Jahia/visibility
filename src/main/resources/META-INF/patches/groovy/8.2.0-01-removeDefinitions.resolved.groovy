import org.jahia.services.content.JCRStoreService
import org.jahia.osgi.FrameworkService
import org.osgi.framework.Bundle

def source = "advanced-visibility";

log.info("Undeploy definitions of " + source);
JCRStoreService jcrStoreService = JCRStoreService.getInstance();
jcrStoreService.undeployDefinitions(source);
log.info("Successfully removed defintions for systemId: " + source);

log.info("Checking if " + source + " bundle is installed");
Bundle[] bundles = FrameworkService.getBundleContext().getBundles();
for (Bundle bundle: bundles) {
    if (bundle.getSymbolicName().equals(source)) {
        log.info("Uninstalling bundle " + source);
        bundle.uninstall();
        log.info("Successfully uninstalled bundle " + source);
    }
}
