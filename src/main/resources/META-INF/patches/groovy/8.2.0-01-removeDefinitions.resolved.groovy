import org.jahia.services.content.JCRStoreService
import org.jahia.osgi.FrameworkService
import org.osgi.framework.Bundle

def source = "advanced-visibility";

log.info("Checking if bundle with symbolic name " + source + " needs to be uninstalled");
Bundle[] bundles = FrameworkService.getBundleContext().getBundles();
for (Bundle bundle: bundles) {
    if (bundle.getSymbolicName().equals(source)) {
        log.info("Bundle " + source + " is present in version " + bundle.getVersion().toString() + ", uninstalling... ");
        bundle.uninstall();
        log.info("Successfully uninstalled bundle " + source);
    }
}

log.info("Undeploy definitions of " + source);
JCRStoreService jcrStoreService = JCRStoreService.getInstance();
jcrStoreService.undeployDefinitions(source);
log.info("Successfully removed defintions for systemId: " + source);
