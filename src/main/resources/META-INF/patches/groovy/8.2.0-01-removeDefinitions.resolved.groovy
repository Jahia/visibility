import org.jahia.services.content.JCRStoreService
import org.jahia.osgi.FrameworkService
import org.jahia.services.content.nodetypes.ExtendedNodeType
import org.jahia.services.content.nodetypes.NodeTypeRegistry
import org.osgi.framework.Bundle

def source = "advanced-visibility";
def target = "visibility";

log.info("Checking if bundle with symbolic name " + source + " needs to be uninstalled");
Bundle[] bundles = FrameworkService.getBundleContext().getBundles();
for (Bundle bundle: bundles) {
    if (bundle.getSymbolicName().equals(source)) {
        log.info("Bundle " + source + " is present in version " + bundle.getVersion().toString() + ", uninstalling... ");
        bundle.uninstall();
        log.info("Successfully uninstalled bundle " + source);
    }
}

def toSwitch = ["jnt:timeOfDayCondition", "jnt:dayOfWeekCondition"];

log.info("Check for nodetypes to switch from " + source + " to " + target + " (" + toSwitch.size() + " nodetypes to switch)");
NodeTypeRegistry nodeTypeRegistry = NodeTypeRegistry.getInstance();
nodeTypeRegistry.getAllNodeTypes(Arrays.asList(source)).forEach { nodeType ->
    if (toSwitch.contains(nodeType.getName())) {
        log.info("Switch nodetype: " + nodeType.getName() + " to " + target);
        def field = ExtendedNodeType.getDeclaredField("systemId")
        field.setAccessible(true)
        try {
            field.set(nodeType, target);
        } finally {
            field.setAccessible(false);
        }
        log.info("Successfully switched nodetype: " + nodeType.getName() + " to " + nodeType.getSystemId());
    }
}

log.info("Undeploy definitions of " + source);
JCRStoreService jcrStoreService = JCRStoreService.getInstance();
jcrStoreService.undeployDefinitions(source);
log.info("Successfully removed definitions for systemId: " + source);
