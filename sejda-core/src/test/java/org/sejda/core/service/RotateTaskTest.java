/*
 * Created on 13/giu/2010
 *
 * Copyright 2010 by Andrea Vacondio (andrea.vacondio@gmail.com).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0 
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 */
package org.sejda.core.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.sejda.TestUtils;
import org.sejda.core.context.DefaultSejdaContext;
import org.sejda.core.context.SejdaContext;
import org.sejda.model.exception.TaskException;
import org.sejda.model.input.PdfStreamSource;
import org.sejda.model.parameter.RotateParameters;
import org.sejda.model.pdf.PdfVersion;
import org.sejda.model.pdf.page.PageRange;
import org.sejda.model.pdf.page.PredefinedSetOfPages;
import org.sejda.model.rotation.Rotation;
import org.sejda.model.task.Task;

import com.lowagie.text.pdf.PdfReader;

/**
 * Abstract test unit for the rotate task
 * 
 * @author Andrea Vacondio
 * 
 */
@Ignore
@SuppressWarnings("unchecked")
public abstract class RotateTaskTest extends PdfOutEnabledTest implements TestableTask<RotateParameters> {

    private DefaultTaskExecutionService victim = new DefaultTaskExecutionService();

    private SejdaContext context = mock(DefaultSejdaContext.class);
    private RotateParameters parameters;

    @Before
    public void setUp() {
        TestUtils.setProperty(victim, "context", context);
    }

    private void setUpDefaultParameters() {
        parameters = new RotateParameters(Rotation.DEGREES_180, PredefinedSetOfPages.ALL_PAGES);
        InputStream stream = getClass().getClassLoader().getResourceAsStream("pdf/test_file.pdf");
        PdfStreamSource source = PdfStreamSource.newInstanceNoPassword(stream, "test_file.pdf");
        parameters.addSource(source);
        parameters.setOverwrite(true);
    }

    private void setUpParametersWithVersionPrefixAndCompressionSpecified() {
        parameters = new RotateParameters(Rotation.DEGREES_180, PredefinedSetOfPages.ALL_PAGES);
        parameters.setCompress(true);
        parameters.setOutputPrefix("test_prefix_");
        parameters.setVersion(PdfVersion.VERSION_1_4);
        InputStream stream = getClass().getClassLoader().getResourceAsStream("pdf/test_file.pdf");
        PdfStreamSource source = PdfStreamSource.newInstanceNoPassword(stream, "test_file.pdf");
        parameters.addSource(source);
        parameters.setOverwrite(true);
    }

    private void setUpRotateSpecificPages() {
        parameters = new RotateParameters(Rotation.DEGREES_90);
        parameters.addPageRange(new PageRange(2, 4));
        InputStream stream = getClass().getClassLoader().getResourceAsStream("pdf/test_file.pdf");
        PdfStreamSource source = PdfStreamSource.newInstanceNoPassword(stream, "test_file.pdf");
        parameters.addSource(source);
        parameters.setOverwrite(true);
    }

    private void setUpRotateMultipleInputNotRangesContained() {
        parameters = new RotateParameters(Rotation.DEGREES_90);
        parameters.addPageRange(new PageRange(2, 4));
        parameters.addPageRange(new PageRange(15, 15));
        parameters.addSource(PdfStreamSource.newInstanceNoPassword(
                getClass().getClassLoader().getResourceAsStream("pdf/test_file.pdf"), "test_file.pdf"));
        parameters.addSource(PdfStreamSource.newInstanceNoPassword(
                getClass().getClassLoader().getResourceAsStream("pdf/medium_test.pdf"), "medium_test.pdf"));
        parameters.setOverwrite(true);
    }

    private void setUpParametersEncrypted() {
        setUpDefaultParameters();

        InputStream stream = getClass().getClassLoader().getResourceAsStream("pdf/enc_with_modify_perm.pdf");
        PdfStreamSource source = PdfStreamSource.newInstanceWithPassword(stream, "test_file.pdf", "test");
        parameters.addSource(source);
    }

    @Test
    public void testExecute() throws TaskException, IOException {
        setUpDefaultParameters();
        doExecute();

        PdfReader reader = getReaderFromResultStream("test_file.pdf");
        assertEquals(4, reader.getNumberOfPages());
        assertEquals(180, reader.getPageRotation(2));
        reader.close();
    }

    @Test
    public void testRotateSpecificPages() throws TaskException, IOException {
        setUpRotateSpecificPages();
        doExecute();

        PdfReader reader = getReaderFromResultStream("test_file.pdf");
        assertEquals(90, reader.getPageRotation(3));
        reader.close();
    }

    @Test
    public void testExecuteEncrypted() throws TaskException, IOException {
        setUpParametersEncrypted();
        doExecute();

        PdfReader reader = getReaderFromResultStream("test_file.pdf");

        assertEquals(4, reader.getNumberOfPages());
        assertEquals(180, reader.getPageRotation(2));
        reader.close();
    }

    @Test
    public void testVersionPrefixAndCreatorAreApplied() throws TaskException, IOException {
        setUpParametersWithVersionPrefixAndCompressionSpecified();

        doExecute();

        PdfReader reader = getReaderFromResultStream("test_prefix_test_file.pdf");
        assertCreator(reader);
        // TODO it seems iText 2 reads the version from the header only while it should read from the catalog first so this assert fails in PDFBox 2
        // assertVersion(reader, PdfVersion.VERSION_1_4);
    }

    @Test
    public void testMultipleInputOneDoesntContainRange() throws TaskException, IOException {
        setUpRotateMultipleInputNotRangesContained();
        doExecute();

        assertOutputContainsDocuments(2);
    }

    private void doExecute() throws TaskException {
        when(context.getTask(parameters)).thenReturn((Task) getTask());
        initializeNewStreamOutput(parameters);
        victim.execute(parameters);
    }

}
