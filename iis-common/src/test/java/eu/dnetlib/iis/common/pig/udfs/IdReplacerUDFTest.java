package eu.dnetlib.iis.common.pig.udfs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.pig.data.DataType;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.logicalLayer.schema.Schema;
import org.apache.pig.impl.logicalLayer.schema.Schema.FieldSchema;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * @author mhorst
 *
 */
public class IdReplacerUDFTest {

    private IdReplacerUDF idReplacer = new IdReplacerUDF();
    
    // --------------------------------- TESTS -------------------------------------
    
    @Test(expected=IllegalArgumentException.class)
    public void testExecOnNull() throws Exception {
        // execute
        idReplacer.exec(null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testExecOnEmpty() throws Exception {
        // given
        TupleFactory tupleFactory = TupleFactory.getInstance();
        
        // execute
        idReplacer.exec(tupleFactory.newTuple());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testExecNotEnoughArgs() throws Exception {
        // given
        TupleFactory tupleFactory = TupleFactory.getInstance();
        
        // execute
        idReplacer.exec(tupleFactory.newTuple(Lists.newArrayList(
                "str1", "str2")));
    }
    
    @Test
    public void testExecSingleIdField() throws Exception {
        // given
        TupleFactory tupleFactory = TupleFactory.getInstance();
        Schema schema = new Schema();
        schema.add(new FieldSchema(null, DataType.CHARARRAY));
        schema.add(new FieldSchema(null, DataType.CHARARRAY));
        schema.add(new FieldSchema("id", DataType.CHARARRAY));
        idReplacer.setInputSchema(schema);
        String idFieldName = "id";
        String newId = "updatedId"; 
        String oldId = "oldId";
        
        // execute
        Tuple result = idReplacer.exec(tupleFactory.newTuple(Lists.newArrayList(
                idFieldName, newId, oldId)));
        assertNotNull(result);
        assertEquals(1, result.getAll().size());
        assertEquals(newId, result.get(0));
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testExecMultipleIdFields() throws Exception {
        // given
        TupleFactory tupleFactory = TupleFactory.getInstance();
        Schema schema = new Schema();
        schema.add(new FieldSchema(null, DataType.CHARARRAY));
        schema.add(new FieldSchema(null, DataType.CHARARRAY));
        schema.add(new FieldSchema("id", DataType.CHARARRAY));
        schema.add(new FieldSchema("id", DataType.CHARARRAY));
        schema.add(new FieldSchema("irrelevant", DataType.BOOLEAN));
        idReplacer.setInputSchema(schema);
        String idFieldName = "id";
        String newId = "updatedId"; 
        String oldId1 = "oldId1";
        String oldId2 = "oldId2";

        // execute
        Tuple result = idReplacer.exec(tupleFactory.newTuple(Lists.newArrayList(
                idFieldName, newId, oldId1, oldId2, true)));
        
        // assert
        assertNotNull(result);
        assertEquals(3, result.getAll().size());
        assertEquals(oldId1, result.get(0));
        assertEquals(newId, result.get(1));
        assertTrue((boolean)result.get(2));
    }

    @Test
    public void testOutputSchema() throws Exception {
     // given
        Schema schema = new Schema();
        schema.add(new FieldSchema(null, DataType.CHARARRAY));
        schema.add(new FieldSchema(null, DataType.CHARARRAY));
        FieldSchema idFieldSchema = new FieldSchema("id", DataType.CHARARRAY);
        schema.add(idFieldSchema);
        FieldSchema dedupFieldSchema = new FieldSchema("dedup", DataType.BOOLEAN);
        schema.add(dedupFieldSchema);
        
        // execute
        Schema resultSchema = idReplacer.outputSchema(schema);
        
        // assert
        assertNotNull(resultSchema);
        assertEquals(1, resultSchema.getFields().size());
        assertEquals(idFieldSchema, resultSchema.getField(0).schema.getField(0));
        assertEquals(dedupFieldSchema, resultSchema.getField(0).schema.getField(1));
    }
    
}
