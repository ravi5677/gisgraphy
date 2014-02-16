/*******************************************************************************
 *   Gisgraphy Project 
 * 
 *   This library is free software; you can redistribute it and/or
 *   modify it under the terms of the GNU Lesser General Public
 *   License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or (at your option) any later version.
 * 
 *   This library is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *   Lesser General Public License for more details.
 * 
 *   You should have received a copy of the GNU Lesser General Public
 *   License along with this library; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307, USA
 * 
 *  Copyright 2008  Gisgraphy project 
 *  David Masclet <davidmasclet@gisgraphy.com>
 *  
 *  
 *******************************************************************************/
package com.gisgraphy.importer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hibernate.FlushMode;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import com.gisgraphy.domain.geoloc.entity.AlternateName;
import com.gisgraphy.domain.geoloc.entity.City;
import com.gisgraphy.domain.geoloc.entity.GisFeature;
import com.gisgraphy.domain.repository.IGisFeatureDao;
import com.gisgraphy.domain.repository.IIdGenerator;
import com.gisgraphy.domain.repository.ISolRSynchroniser;
import com.gisgraphy.domain.valueobject.AlternateNameSource;
import com.gisgraphy.domain.valueobject.GISSource;
import com.gisgraphy.domain.valueobject.NameValueDTO;
import com.gisgraphy.domain.valueobject.Output;
import com.gisgraphy.domain.valueobject.Output.OutputStyle;
import com.gisgraphy.fulltext.FullTextSearchEngine;
import com.gisgraphy.helper.GeolocHelper;
import com.vividsolutions.jts.geom.Point;

/**
 * Import the POI from an (pre-processed) openStreet map data file.
 * The goal of this importer is to cross information between geonames and Openstreetmap. 
 * 
 * 
 * @author <a href="mailto:david.masclet@gisgraphy.com">David Masclet</a>
 */
public class OpenStreetMapPoisSimpleImporter extends AbstractSimpleImporterProcessor {
	
    
    public static final Output MINIMUM_OUTPUT_STYLE = Output.withDefaultFormat().withStyle(OutputStyle.SHORT);
    
    public static final String ALTERNATENAMES_EXTRACTION_REGEXP = "((?:(?!___).)+)(?:(?:___)|(?:$))";
    
    public static final Pattern ALTERNATENAMES_EXTRACTION_PATTERN = Pattern.compile(ALTERNATENAMES_EXTRACTION_REGEXP);

    @Autowired
	protected IIdGenerator idGenerator;
    
    @Autowired
    protected IGisFeatureDao gisFeatureDao;
    
    @Autowired
    protected ISolRSynchroniser solRSynchroniser;
  
    
    OsmAmenityToPlacetype osmAmenityToPlacetype = new OsmAmenityToPlacetype();
    

    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#flushAndClear()
     */
    @Override
    protected void flushAndClear() {
    	gisFeatureDao.flushAndClear();
    }
    
    @Override
    protected void setup() {
        super.setup();
        //temporary disable logging when importing
        FullTextSearchEngine.disableLogging=true;
        idGenerator.sync();
    }
    

    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#getFiles()
     */
    @Override
    protected File[] getFiles() {
    	return ImporterHelper.listCountryFilesToImport(importerConfig.getOpenStreetMapPoisDir());
    }

    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#getNumberOfColumns()
     */
    @Override
    protected int getNumberOfColumns() {
    	return 7;
    }

    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#processData(java.lang.String)
     */
    @Override
    protected void processData(String line) throws ImporterException {
	String[] fields = line.split("\t");
	String amenityFields = null;
	
	//
	// Line table has the following fields :
	// --------------------------------------------------- 
	//0 : Node type; 1 : id; 2 : name; 3 : countrycode;4 : alternatenames; 
	//5 : location,	6 : amenity;
	//
	//
	checkNumberOfColumn(fields);
	//amenity
	if (!isEmptyField(fields, 6, true)) {
			amenityFields=fields[6].trim();
	}
	
	List<GisFeature> pois = createAndpopulatePoi(fields,amenityFields);
	if (pois == null){
		return;
	}
	try {
		for (GisFeature poi:pois){
			gisFeatureDao.save(poi);
		}
	} catch (ConstraintViolationException e) {
		logger.error("Can not save "+dumpFields(fields)+"(ConstraintViolationException) we continue anyway but you should consider this",e);
	}catch (Exception e) {
		logger.error("Can not save "+dumpFields(fields)+" we continue anyway but you should consider this",e);
	}

    }

    
	List<GisFeature> createAndpopulatePoi(String[] fields, String amenity) {
		String[] tags = splitTags(amenity);
		List<GisFeature> pois = osmAmenityToPlacetype.getObjectsFromTags(tags);
		for (GisFeature poi:pois){
		poi.setSource(GISSource.OPENSTREETMAP);
		//osmId
		if (!isEmptyField(fields, 1, true)) {
			String osmIdAsString =fields[1].trim();
			Long osmId;
			try {
				osmId = Long.parseLong(osmIdAsString);
				poi.setOpenstreetmapId(osmId);
			} catch (NumberFormatException e) {
				logger.error("can not parse openstreetmap id "+osmIdAsString);
				return null;
			}
		}
		
		// name
		if (!isEmptyField(fields, 2, false)) {
		   String  name=fields[2].trim();
		    if (name==null){
		    	return null;
		    }
		    poi.setName(name);
		}
		//countrycode
		if (!isEmptyField(fields, 3, true)) {
			String countryCode=fields[3].trim().toUpperCase();
			poi.setCountryCode(countryCode);
		}
		
		//populate alternatenames
		if (!isEmptyField(fields, 4, false)) {
			String alternateNamesAsString=fields[4].trim();
			populateAlternateNames(poi,alternateNamesAsString);
		}
		
		//location
		if (!isEmptyField(fields, 5, false)) {
			try {
				Point location = (Point) GeolocHelper.convertFromHEXEWKBToGeometry(fields[5]);
				poi.setLocation(location);
			} catch (RuntimeException e) {
				logger.warn("can not parse location for "+fields[6]+" : "+e);
				return null;
			}
		}
				
		
		//featureId
		poi.setFeatureId(idGenerator.getNextFeatureId());
		}
		return pois;
	}

	protected String[] splitTags(String amenity) {
		String[] tags= new String[14];
		String[] tagsvalues = amenity.split("___");
		//System.out.println(tagsvalues.length);
		for (int j =0;j<tagsvalues.length;j++){
		//	System.err.println(j+"="+tagsvalues[j]);
			if (!"".equals(tagsvalues[j].trim())){
				tags[j]=tagsvalues[j];
			}
		}
		return tags;
	}



	GisFeature populateAlternateNames(GisFeature poi,
			String alternateNamesAsString) {
		if (poi ==null || alternateNamesAsString ==null){
			return poi;
		}
		Matcher matcher = ALTERNATENAMES_EXTRACTION_PATTERN.matcher(alternateNamesAsString);
		int i = 0;
		while (matcher.find()){
			if (matcher.groupCount() != 1) {
				logger.warn("wrong number of fields for alternatename no " + i + "for line " + alternateNamesAsString);
				continue;
			}
			String alternateName = matcher.group(1);
			if (alternateName!= null && !"".equals(alternateName.trim())){
				poi.addAlternateName(new AlternateName(alternateName,AlternateNameSource.OPENSTREETMAP));
			}
		}
		return poi;
		
	}

	/* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#shouldBeSkiped()
     */
    @Override
    public boolean shouldBeSkipped() {
    	return !importerConfig.isOpenstreetmapImporterEnabled();
    }
    
   


    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#setCommitFlushMode()
     */
    @Override
    protected void setCommitFlushMode() {
    	this.gisFeatureDao.setFlushMode(FlushMode.COMMIT);
    }

    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#shouldIgnoreComments()
     */
    @Override
    protected boolean shouldIgnoreComments() {
    	return true;
    }

    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.AbstractImporterProcessor#shouldIgnoreFirstLine()
     */
    @Override
    protected boolean shouldIgnoreFirstLine() {
    	return false;
    }

    /* (non-Javadoc)
     * @see com.gisgraphy.domain.geoloc.importer.IGeonamesProcessor#rollback()
     */
    public List<NameValueDTO<Integer>> rollback() {
    	List<NameValueDTO<Integer>> deletedObjectInfo = new ArrayList<NameValueDTO<Integer>>();
    	logger.info("reseting openstreetmap cities...");
    	//TODO only POI that have source openstreetmap
    	    deletedObjectInfo
    		    .add(new NameValueDTO<Integer>(City.class.getSimpleName(), 0));
    	resetStatus();
    	return deletedObjectInfo;
    }
    
    
    
    @Override
    //TODO test
    protected void tearDown() {
    	super.tearDown();
    	String savedMessage = this.statusMessage;
    	try {
    		 FullTextSearchEngine.disableLogging=true;
    		this.statusMessage = internationalisationService.getString("import.fulltext.optimize");
    		solRSynchroniser.optimize();
    	} finally {
    	    // we restore message in case of error
    	    this.statusMessage = savedMessage;
    	}
    }
    
    
   

    @Required
    public void setSolRSynchroniser(ISolRSynchroniser solRSynchroniser) {
        this.solRSynchroniser = solRSynchroniser;
    }

    @Required
    public void setIdGenerator(IIdGenerator idGenerator) {
        this.idGenerator = idGenerator;
    }

	public void setGisFeatureDao(IGisFeatureDao gisFeatureDao) {
		this.gisFeatureDao = gisFeatureDao;
	}

    
    

    
}
