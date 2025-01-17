package eu.dnetlib.iis.common.model.extrainfo.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.Test;

import eu.dnetlib.iis.common.model.extrainfo.citations.BlobCitationEntry;
import eu.dnetlib.iis.common.model.extrainfo.citations.TypedId;

/**
 * @author mhorst
 *
 */
public class CitationsExtraInfoConverterTest {

    CitationsExtraInfoConverter converter = new CitationsExtraInfoConverter();
    
    @Test
    public void testSerializeNull() throws Exception {
        String result = converter.serialize(null);
        assertEquals("<null/>", result);
    }
    
    @Test(expected=NullPointerException.class)
    public void testDeserializeNull() throws Exception {
        converter.deserialize(null);
    }
    
    @Test
    public void testGetXStream() throws Exception {
        assertNotNull(converter.getXstream());
    }
    
    @Test
    public void testSerializeAndDeserialize() throws Exception {
        // given
        int position = 1;
        String rawText = "citation raw text";
        
        String idValue = "val";
        String idType = "type";
        float confidenceLevel = 0.9f;
        
        List<TypedId> identifiers = new ArrayList<>();
        identifiers.add(new TypedId(idValue, idType, confidenceLevel));
        
        BlobCitationEntry entry = new BlobCitationEntry();
        entry.setPosition(position);
        entry.setRawText(rawText);
        entry.setIdentifiers(identifiers);
        
        SortedSet<BlobCitationEntry> entrySet = new TreeSet<>();
        entrySet.add(entry);
        
        // execute
        String resultStr = converter.serialize(entrySet);
        SortedSet<BlobCitationEntry> resultSet = converter.deserialize(resultStr);
        
        // assert
        assertNotNull(resultSet);
        assertEquals(1, resultSet.size());
        BlobCitationEntry resultEntry = resultSet.first();
        assertNotNull(resultEntry);
        assertEquals(position, resultEntry.getPosition());
        assertEquals(rawText, resultEntry.getRawText());
        assertNotNull(resultEntry.getIdentifiers());
        assertEquals(1, resultEntry.getIdentifiers().size());
        TypedId resultId = resultEntry.getIdentifiers().get(0);
        assertEquals(idValue, resultId.getValue());
        assertEquals(idType, resultId.getType());
        assertEquals(confidenceLevel, resultId.getConfidenceLevel(), 0.001f);
    }

}
