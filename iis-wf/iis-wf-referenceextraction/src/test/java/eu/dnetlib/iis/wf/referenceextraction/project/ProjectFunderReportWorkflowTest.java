package eu.dnetlib.iis.wf.referenceextraction.project;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import eu.dnetlib.iis.common.AbstractOozieWorkflowTestCase;
import eu.dnetlib.iis.common.IntegrationTest;
import eu.dnetlib.iis.common.OozieWorkflowTestConfiguration;

/**
 * 
 * @author mhorst
 *
 */
@Category(IntegrationTest.class)
public class ProjectFunderReportWorkflowTest extends AbstractOozieWorkflowTestCase {

    @Test
	public void testWorkflow() throws Exception {
        OozieWorkflowTestConfiguration wf = new OozieWorkflowTestConfiguration();
        wf.setTimeoutInSeconds(720);
        testWorkflow("eu/dnetlib/iis/wf/referenceextraction/project/funder_report/test", wf);
    }

}
