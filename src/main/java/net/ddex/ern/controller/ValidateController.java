package net.ddex.ern.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import javafx.util.Pair;
import net.ddex.ern.exception.ValidatorException;
import net.ddex.ern.service.SchemaService;
import net.ddex.ern.service.SchematronService;
import net.ddex.ern.vo.ValidationResponse;

@RestController
public class ValidateController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidateController.class);
    private static String PROFILE_ROOT = "profiles/";

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SchematronService schematronService;

    @GetMapping(path = "/status", produces = "text/plain")
    public String test() {
        return "The service is running";
    }

    @PostMapping(path = "/json/validate", produces = "application/json")
    public List<Map<String, Object>> validate(@RequestParam(value = "messageFile") MultipartFile file,
                                              @RequestParam(value = "messageSchemaVersionId") Optional<String> schemaVersion,
                                              @RequestParam(value = "releaseProfileVersionId") Optional<String> profileVersion)
            throws ParserConfigurationException, IOException, XMLStreamException, TransformerException,
            SAXException, XPathExpressionException, ValidatorException {

        List<Map<String, Object>> list = new ArrayList<>(2);
        Map<String, Object> map = new HashMap<>(2);

        String _schemaVersion = schemaVersion.isPresent() ? schemaVersion.get().replace(".", "") : "";
        _schemaVersion = _schemaVersion.startsWith("/") ? _schemaVersion.substring(1) : _schemaVersion;
        _schemaVersion = _schemaVersion.endsWith("/") ? _schemaVersion.substring(0, _schemaVersion.length() - 1) : _schemaVersion;

        String _profileVersion = profileVersion.isPresent() ? profileVersion.get().replace(".", "") : "";
        _profileVersion = _profileVersion.startsWith("/") ? _profileVersion.substring(1) : _profileVersion;
        _profileVersion = _profileVersion.endsWith("/") ? _profileVersion.substring(0, _profileVersion.length() -1) : _profileVersion;

        try {
            Pair<String, String> spec = schemaService.validateSchema(file.getInputStream(), _schemaVersion, _profileVersion);
            map.put("schema", "Message vaildates against schema version " + spec.getKey());
            _profileVersion = _profileVersion.isEmpty() ? spec.getValue() : String.format("%s%s%s", PROFILE_ROOT, _profileVersion, ".xsl");
        } catch (SAXException e) {
            LOGGER.error(e.getMessage());
            map.put("schema", e.getMessage());
            list.add(map);
            return list;
        }
        
        map.put("schematron", schematronService.schematron2Map(file.getInputStream(), _profileVersion));
        list.add(map);
        return list;
    }

    @ExceptionHandler
    public ValidationResponse handleValidatorException(ValidatorException ex) {
        LOGGER.error(ex.getMessage());
        return new ValidationResponse(ex.getStatus(), ex.getErrorMessage());
    }
}