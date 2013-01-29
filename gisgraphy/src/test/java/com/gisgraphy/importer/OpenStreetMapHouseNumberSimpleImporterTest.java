package com.gisgraphy.importer;

import java.util.ArrayList;
import java.util.List;

import net.sf.jstester.util.Assert;

import org.junit.Test;

import com.gisgraphy.helper.GeolocHelper;
import com.gisgraphy.importer.dto.AddressInclusion;
import com.gisgraphy.importer.dto.AssociatedStreetHouseNumber;
import com.gisgraphy.importer.dto.AssociatedStreetMember;
import com.gisgraphy.importer.dto.InterpolationHouseNumber;
import com.gisgraphy.importer.dto.InterpolationMember;
import com.gisgraphy.importer.dto.InterpolationType;
import com.gisgraphy.importer.dto.NodeHouseNumber;
import com.vividsolutions.jts.geom.Point;

public class OpenStreetMapHouseNumberSimpleImporterTest {

	@Test
	public void parseAssociatedStreetHouseNumber() {
		String line = "A	" +
				"2069647	1661205474___0101000020E6100000046DBC85BFA81D40DA7D22AA4BDD4540___24___Avenue de Fontvieille___N___house___" +
				"158189815___0101000020E61000002AA4070C99A81D40227F492749DD4540___Avenue de Fontvieille___Avenue de Fontvieille___W___street___" +
				"176577460___0101000020E61000004522EE9504A81D4081BAA66957DD4540___Avenue de Fontvieille___Avenue de Fontvieille___W___street";
		OpenStreetMapHouseNumberSimpleImporter importer = new OpenStreetMapHouseNumberSimpleImporter();
			AssociatedStreetHouseNumber actual = importer.parseAssociatedStreetHouseNumber(line);
			//TODO wrong number of fields, null
			Assert.assertEquals("2069647", actual.getRelationID());
			Assert.assertNotNull(actual.getAssociatedStreetMember());
			Assert.assertEquals(3,actual.getAssociatedStreetMember().size());
			AssociatedStreetMember m1 = new AssociatedStreetMember("1661205474", (Point)GeolocHelper.convertFromHEXEWKBToGeometry("0101000020E6100000046DBC85BFA81D40DA7D22AA4BDD4540"),"24",null,"N","house");
			AssociatedStreetMember m2 = new AssociatedStreetMember("158189815", (Point)GeolocHelper.convertFromHEXEWKBToGeometry("0101000020E61000002AA4070C99A81D40227F492749DD4540"),null,"Avenue de Fontvieille","W","street");
			AssociatedStreetMember m3 = new AssociatedStreetMember("176577460", (Point)GeolocHelper.convertFromHEXEWKBToGeometry("0101000020E61000004522EE9504A81D4081BAA66957DD4540"),null,"Avenue de Fontvieille","W","street");
			
			Assert.assertTrue(actual.getAssociatedStreetMember().contains(m1));
			Assert.assertTrue(actual.getAssociatedStreetMember().contains(m2));
			Assert.assertTrue(actual.getAssociatedStreetMember().contains(m3));
	}
	
	@Test
	public void parseAssociatedStreetHouseNumberWrongType() {
		String line = "X	" +
				"2069647	1661205474___0101000020E6100000046DBC85BFA81D40DA7D22AA4BDD4540___24___Avenue de Fontvieille___N___house___" +
				"158189815___0101000020E61000002AA4070C99A81D40227F492749DD4540___Avenue de Fontvieille___Avenue de Fontvieille___W___street___" +
				"176577460___0101000020E61000004522EE9504A81D4081BAA66957DD4540___Avenue de Fontvieille___Avenue de Fontvieille___W___street";
		OpenStreetMapHouseNumberSimpleImporter importer = new OpenStreetMapHouseNumberSimpleImporter();
			Assert.assertNull(importer.parseAssociatedStreetHouseNumber(line));
	}
	
	@Test
	public void parseAssociatedStreetHouseNumberUnderscoreInName() {
		String line = "A	" +
				"2069647	1661205474___0101000020E6100000046DBC85BFA81D40DA7D22AA4BDD4540___24___Ave_nue de Fontvieille___N___house___" +
				"176577460___0101000020E61000004522EE9504A81D4081BAA66957DD4540___Avenue de Fontvieil_le___Avenue_ de Fontvieille___W___street";
		OpenStreetMapHouseNumberSimpleImporter importer = new OpenStreetMapHouseNumberSimpleImporter();
			AssociatedStreetHouseNumber actual = importer.parseAssociatedStreetHouseNumber(line);
			//TODO wrong number of fields, null
			Assert.assertEquals("2069647", actual.getRelationID());
			Assert.assertNotNull(actual.getAssociatedStreetMember());
			Assert.assertEquals(2,actual.getAssociatedStreetMember().size());
			AssociatedStreetMember m1 = new AssociatedStreetMember("1661205474", (Point)GeolocHelper.convertFromHEXEWKBToGeometry("0101000020E6100000046DBC85BFA81D40DA7D22AA4BDD4540"),"24",null,"N","house");
			AssociatedStreetMember m2 = new AssociatedStreetMember("176577460", (Point)GeolocHelper.convertFromHEXEWKBToGeometry("0101000020E61000004522EE9504A81D4081BAA66957DD4540"),null,"Avenue de Fontvieil_le","W","street");
			
			Assert.assertTrue(actual.getAssociatedStreetMember().contains(m1));
			Assert.assertTrue(actual.getAssociatedStreetMember().contains(m2));
	}
	
	
	@Test
	public void parseAssociatedStreetHouseNumberWrongHouseNumberType() {
		String line = "B	" +
				"2069647	1661205474___0101000020E6100000046DBC85BFA81D40DA7D22AA4BDD4540___24___Avenue de Fontvieille___N___house___" +
				"158189815___0101000020E61000002AA4070C99A81D40227F492749DD4540___Avenue de Fontvieille___Avenue de Fontvieille___W___street___" +
				"176577460___0101000020E61000004522EE9504A81D4081BAA66957DD4540___Avenue de Fontvieille___Avenue de Fontvieille___W___street";
		OpenStreetMapHouseNumberSimpleImporter importer = new OpenStreetMapHouseNumberSimpleImporter();
		Assert.assertNull(importer.parseAssociatedStreetHouseNumber(line));
	}
	
	@Test
	public void parseAssociatedStreetHouseNumberWrongNumberOFields() {
		String line = "A	" +
				"2069647	";
		OpenStreetMapHouseNumberSimpleImporter importer = new OpenStreetMapHouseNumberSimpleImporter();
		Assert.assertNull(importer.parseAssociatedStreetHouseNumber(line));
	}
	
	@Test
	public void parseAssociatedStreetHouseNumberNullOrEmptyLine() {
		String line =null;
		OpenStreetMapHouseNumberSimpleImporter importer = new OpenStreetMapHouseNumberSimpleImporter();
			Assert.assertNull(importer.parseAssociatedStreetHouseNumber(line));
			Assert.assertNull(importer.parseAssociatedStreetHouseNumber(""));
	}
	
	/*--------------------------------------------------interpolation-----------------------------------------------*/
	@Test
	public void parseInterpolationHouseNumberWrongNumberOFields() {
		String line = "I	" +
				"2069647	";
		OpenStreetMapHouseNumberSimpleImporter importer = new OpenStreetMapHouseNumberSimpleImporter();
		Assert.assertNull(importer.parseInterpolationHouseNumber(line));
	}
	
	@Test
	public void parseInterpolationHouseNumberNullOrEmptyLine() {
		String line =null;
		OpenStreetMapHouseNumberSimpleImporter importer = new OpenStreetMapHouseNumberSimpleImporter();
			Assert.assertNull(importer.parseInterpolationHouseNumber(line));
			Assert.assertNull(importer.parseInterpolationHouseNumber(""));
	}
	
	@Test
	public void parseInterpolationHouseNumber() {
		String line = "I	168365171	1796478450___0___0101000020E61000009A023EE4525350C0959C137B682F38C0_________"
				+"1796453793___2___0101000020E610000038691A144D5350C023ADE75A6A2F38C0___600___ba_r___"
		  +"1366275082___1___0101000020E610000068661CD94B5350C0B055270C6F2F38C0______foo___"
		 +"1796453794___3___0101000020E6100000F38F6390605350C028A6666A6D2F38C0___698___	sname	even	actual";
		OpenStreetMapHouseNumberSimpleImporter importer = new OpenStreetMapHouseNumberSimpleImporter();
			InterpolationHouseNumber interpolation = importer.parseInterpolationHouseNumber(line);
			InterpolationHouseNumber actual = interpolation;
			//TODO wrong number of fields, null
			Assert.assertEquals("168365171", actual.getWayId());
			Assert.assertEquals(InterpolationType.even,actual.getInterpolationType());
			Assert.assertEquals("sname",actual.getStreetName());
			Assert.assertEquals(AddressInclusion.actual,actual.getAddressInclusion());
			Assert.assertEquals(4,actual.getMembers().size());
			InterpolationMember m0 = new InterpolationMember("1796478450", 0, (Point)GeolocHelper.convertFromHEXEWKBToGeometry("0101000020E61000009A023EE4525350C0959C137B682F38C0"),null,null);
			InterpolationMember m1 = new InterpolationMember("1366275082", 1, (Point)GeolocHelper.convertFromHEXEWKBToGeometry("0101000020E610000068661CD94B5350C0B055270C6F2F38C0"),null,"foo");
			InterpolationMember m2 = new InterpolationMember("1796453793", 2, (Point)GeolocHelper.convertFromHEXEWKBToGeometry("0101000020E610000038691A144D5350C023ADE75A6A2F38C0"),"600","ba_r");
			InterpolationMember m3 = new InterpolationMember("1796453794", 3, (Point)GeolocHelper.convertFromHEXEWKBToGeometry("0101000020E6100000F38F6390605350C028A6666A6D2F38C0"),"698",null);
			
			Assert.assertEquals("members should be sorted",m0,actual.getMembers().get(0));
			Assert.assertEquals(m1,actual.getMembers().get(1));
			Assert.assertEquals(m2,actual.getMembers().get(2));
			Assert.assertEquals(m3,actual.getMembers().get(3));
			//other common test
			line = "I	168365171	1796478450___0___0101000020E61000009A023EE4525350C0959C137B682F38C0_________"
					+"1796453793___2___0101000020E610000038691A144D5350C023ADE75A6A2F38C0___600___ba_r___"
			  +"1366275082___1___0101000020E610000068661CD94B5350C0B055270C6F2F38C0______foo___"
			 +"1796453794___3___0101000020E6100000F38F6390605350C028A6666A6D2F38C0___698___	sname	even	";
			interpolation = importer.parseInterpolationHouseNumber(line);
			Assert.assertNotNull("address inclusion is empty",interpolation);
			Assert.assertEquals(4,interpolation.getMembers().size());
				
			line ="I	169508709	1806691488___0___0101000020E610000045C3BD8D28404DC0E08211A04B4D41C0___3702___Tinogasta___1806691490___1___0101000020E6100000C3FF0C2549404DC0F924C1655F4D41C0___3800___Tinogasta		even	actual";
			Assert.assertNotNull(interpolation);
			Assert.assertEquals(4,interpolation.getMembers().size());
	}
	
	//parseNodeHouseNumber
	@Test
	public void parseNodeHouseNumberWrongNumberOFields() {
		String line = "N	" +
				"2069647	";
		OpenStreetMapHouseNumberSimpleImporter importer = new OpenStreetMapHouseNumberSimpleImporter();
		Assert.assertNull(importer.parseNodeHouseNumber(line));
	}
	
	@Test
	public void parseNodeHouseNumberNullOrEmptyLine() {
		String line =null;
		OpenStreetMapHouseNumberSimpleImporter importer = new OpenStreetMapHouseNumberSimpleImporter();
		Assert.assertNull(importer.parseNodeHouseNumber(line));
		Assert.assertNull(importer.parseNodeHouseNumber(""));
	}
	
	@Test
	public void parseNodeHouseNumber(){
		String line = "N	247464344	0101000020E610000044BC1A457B304DC018737C597F4B41C0	405	Museo Ferroviario	Avenida Del Libertador";
		OpenStreetMapHouseNumberSimpleImporter importer = new OpenStreetMapHouseNumberSimpleImporter();
		NodeHouseNumber actual = importer.parseNodeHouseNumber(line);
		NodeHouseNumber expected = new NodeHouseNumber("247464344",(Point)GeolocHelper.convertFromHEXEWKBToGeometry("0101000020E610000044BC1A457B304DC018737C597F4B41C0"),"405","Museo Ferroviario","Avenida Del Libertador") ;
		Assert.assertEquals(expected, actual);
	}
	
	@Test
	public void segmentize(){
		/*
		0     5           15
		|------|------|------|------|-----|
		0      6     12      18     24     30
		*/
		OpenStreetMapHouseNumberSimpleImporter importer = new OpenStreetMapHouseNumberSimpleImporter();
		Point p1 = GeolocHelper.createPoint(1f, 1f);
		Point p2 = GeolocHelper.createPoint(1f, 1.045225f);
		Point p3 = GeolocHelper.createPoint(1f, 1.13567f);
		Point p4 = GeolocHelper.createPoint(1f, 1.27133f);
		/*
		System.out.println(GeolocHelper.distance(p1, p2));
		System.out.println(GeolocHelper.distance(p2, p3));
		System.out.println(GeolocHelper.distance(p3, p4));*/
		List<Point> points = new ArrayList<Point>();
		points.add(p1);
		points.add(p2);
		points.add(p3);
		points.add(p4);
		
		List<Point> result = importer.segmentize(points, 4);
		Assert.assertEquals(6, result.size());
		for (int i =0; i<5;i++){
			double distance = GeolocHelper.distance(result.get(i), result.get(i+1));
			System.out.println(distance);
			Assert.assertTrue(Math.abs(distance-6000)<10);
		}
		
		
	}
	

}