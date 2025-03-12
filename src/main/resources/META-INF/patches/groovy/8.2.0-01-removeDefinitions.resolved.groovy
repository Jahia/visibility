import org.jahia.services.content.JCRStoreService

def source = "advanced-visibility";

log.info("Undeploy definitions of " + source);
JCRStoreService jcrStoreService = JCRStoreService.getInstance();
jcrStoreService.undeployDefinitions(source);
log.info("Successfully removed defintions for systemId: " + source);
