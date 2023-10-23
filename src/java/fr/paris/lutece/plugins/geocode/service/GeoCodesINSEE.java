/*
 * Copyright (c) 2002-2023, City of Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.geocode.service;

import java.sql.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.geocodes.business.City;
import fr.paris.lutece.plugins.geocodes.business.Country;
import fr.paris.lutece.plugins.geocodes.provider.IGeoCodeProvider;
import fr.paris.lutece.plugins.geocodes.rs.Constants;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.httpaccess.HttpAccess;
import fr.paris.lutece.util.httpaccess.HttpAccessException;
import fr.paris.lutece.util.httpaccess.InvalidResponseStatus;
import io.jsonwebtoken.lang.Arrays;

public class GeoCodesINSEE implements IGeoCodeProvider
{

    private static final String PROPERTY_API_INSEE_BASE_UR_COMMUNE = "geocodes.api.insee.url.commune";
    private static final String PROPERTY_API_INSEE_BASE_UR_COMMUNES_LIST = "geocodes.api.insee.url.list.communes";
    private static final String PROPERTY_API_INSEE_BASE_URL_COMMUNES_PREVIOUS_LIST = "geocodes.api.insee.url.list.previous.communes";
    private static final String PROPERTY_API_INSEE_BASE_URL_ALL_COMMUNES_LIST = "geocodes.api.insee.url.list.all.communes";
    private static final String PROPERTY_API_INSEE_BASE_URL_COUNTRY = "geocodes.api.insee.url.country";
    private static final String PROPERTY_API_INSEE_BASE_URL_TOKEN = "geocodes.api.insee.url.token";
    private static final String PROPERTY_API_INSEE_AUTH_CLIENT_ID = "geocodes.api.insee.url.client.id";
    private static final String PROPERTY_API_INSEE_AUTH_CLIENT_SECRET = "geocodes.api.insee.url.client.secret";
    
    private static final String PARAMETER_CLIENT_ID = "client_id";
    private static final String PARAMETER_CLIENT_SECRET = "client_secret";
    private static final String PARAMETER_DATE = "date";

    private static final String TYPE_AUTHENTIFICATION_BASIC = "Basic";

    private static Logger _logger = Logger.getLogger( "lutece.awx" );

    @Override
    public String getId( )
    {
        return Constants.ID_PROVIDER_GEOCODE_INSEE;
    }

    /**
     * @param strRelativeUri
     * @return
     * @throws HttpAccessException
     */
    public String doGetJson( String strRelativeUri, String strUrlPath, String strToken )
    {
        String strJsonResult = null;

        try
        {
            HttpAccess httpAccess = new HttpAccess( );

            HashMap<String, String> mapHeader = new HashMap<>( );
            mapHeader.put( "Authorization", "Bearer " + strToken );
            mapHeader.put( "Accept", "application/json" );

            String strUrl = strUrlPath + strRelativeUri;
            strJsonResult = httpAccess.doGet( strUrl, null, null, mapHeader, null );
        }
        catch( HttpAccessException e )
        {
            if ( e instanceof InvalidResponseStatus )
            {
                InvalidResponseStatus response = (InvalidResponseStatus) e;
                if ( response.getResponseStatus( ) != 404 )
                {
                    String strError = "API INSEE - Error calling '" + strRelativeUri + "' : ";
                    _logger.error( strError + e.getMessage( ), e );
                }
                else
                {
                    _logger.info( " No Update for city : " + strRelativeUri );
                }
            }

        }

        return strJsonResult;
    }

    public Optional<City> getCityByDateAndCode( Date dateCity, String strCode )
    {
        String strUrl = AppPropertiesService.getProperty( PROPERTY_API_INSEE_BASE_UR_COMMUNE );
        StringBuilder strValueUrl = new StringBuilder( strCode );
        strValueUrl.append( "?").append( PARAMETER_DATE ).append("=" ).append( dateCity );

        String strCityJson = "";
        String strToken = getToken( AppPropertiesService.getProperty( PROPERTY_API_INSEE_BASE_URL_TOKEN ) );
        strCityJson = doGetJson( strValueUrl.toString( ), strUrl, strToken );

        ObjectMapper objectMapper = new ObjectMapper( );
        objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        City city = null;
        if ( strCityJson != null && !strCityJson.isEmpty( ) )
        {
            try
            {
                city = objectMapper.readValue( strCityJson, City.class );
            }
            catch( JsonProcessingException e )
            {
                String strError = "API INSEE - Error converting to Object from JSON '" + strCityJson + "' : ";
                _logger.error( strError + e.getMessage( ), e );
            }
        }

        return Optional.ofNullable( city );
    }

    public List<City> getCitiesListByNameAndDate( String strSearchBeginningVal, Date dateCity )
    {
        String strUrl = AppPropertiesService.getProperty( PROPERTY_API_INSEE_BASE_UR_COMMUNES_LIST );
        StringBuilder strValueUrl = new StringBuilder( "?filtreNom=" );
        // String strValueUrl = "?filtreNom=" + strSearchBeginningVal + "&date=" + dateCity;
        strValueUrl.append( strSearchBeginningVal ).append( "&" ).append( PARAMETER_DATE ).append( "=" ).append( dateCity );
        String strCityJson = "";
        String strToken = getToken( AppPropertiesService.getProperty( PROPERTY_API_INSEE_BASE_URL_TOKEN ) );
        strCityJson = doGetJson( strValueUrl.toString( ), strUrl, strToken );

        ObjectMapper objectMapper = new ObjectMapper( );
        objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        City [ ] cities = null;
        if ( strCityJson != null && !strCityJson.isEmpty( ) )
        {
            try
            {
                cities = objectMapper.readValue( strCityJson, City [ ].class );
            }
            catch( JsonProcessingException e )
            {
                String strError = "API INSEE - Error converting to Object from JSON '" + strCityJson + "' : ";
                _logger.error( strError + e.getMessage( ), e );
            }
        }

        return Arrays.asList( cities );
    }

    public Optional<Country> getCountryByCodeAndDate( Date dateCountry, String strCodePays )
    {
        String strUrl = AppPropertiesService.getProperty( PROPERTY_API_INSEE_BASE_URL_COUNTRY );
        StringBuilder strValueUrl = new StringBuilder( strCodePays );
        strValueUrl.append( "?" ).append( PARAMETER_DATE ).append( "=" ).append( dateCountry );

        String strCountryJson = "";
        String strToken = getToken( AppPropertiesService.getProperty( PROPERTY_API_INSEE_BASE_URL_TOKEN ) );
        strCountryJson = doGetJson( strValueUrl.toString( ), strUrl, strToken );

        ObjectMapper objectMapper = new ObjectMapper( );
        objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        Country country = null;
        if ( strCountryJson != null && !strCountryJson.isEmpty( ) )
        {
            try
            {
                country = objectMapper.readValue( strCountryJson, Country.class );
            }
            catch( JsonProcessingException e )
            {
                String strError = "API INSEE - Error converting to Object from JSON '" + strCountryJson + "' : ";
                _logger.error( strError + e.getMessage( ), e );
            }
        }

        return Optional.ofNullable( country );
    }
    
    public String getToken( String strUrl )
    {
        String strToken = "";

        String strJsonResult = null;
        String strAuthId = AppPropertiesService.getProperty( PROPERTY_API_INSEE_AUTH_CLIENT_ID );
        String strAuthSecret = AppPropertiesService.getProperty( PROPERTY_API_INSEE_AUTH_CLIENT_SECRET );

        try
        {
            HttpAccess httpAccess = new HttpAccess( );

            HashMap<String, String> mapHeader = new HashMap<>( );
            mapHeader.put( PARAMETER_CLIENT_ID, strAuthId );
            mapHeader.put( PARAMETER_CLIENT_SECRET, strAuthSecret );

            mapHeader.put( HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON );
            mapHeader.put( "grant_type", "client_credentials" );

            strJsonResult = httpAccess.doPost( strUrl, mapHeader );

            ObjectMapper objectMapper = new ObjectMapper( );
            objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );

            JsonNode root = objectMapper.readTree( strJsonResult );
            strToken = root.at( "/access_token" ).asText( );
        }
        catch( HttpAccessException e )
        {
            String strError = "API INSEE - Error calling '" + strUrl + "' : ";
            _logger.error( strError + e.getMessage( ), e );
        }
        catch( JsonMappingException e )
        {
            String strError = "API INSEE - Error JSON mapping '" + strUrl + "' : ";
            _logger.error( strError + e.getMessage( ), e );
        }
        catch( JsonProcessingException e )
        {
            String strError = "API INSEE - Error JSON processing '" + strUrl + "' : ";
            _logger.error( strError + e.getMessage( ), e );
        }

        return strToken;
    }

    public List<City> getAllCities( )
    {
        String strUrl = AppPropertiesService.getProperty( PROPERTY_API_INSEE_BASE_URL_ALL_COMMUNES_LIST );
        String strCityJson = "";
        String strToken = getToken( AppPropertiesService.getProperty( PROPERTY_API_INSEE_BASE_URL_TOKEN ) );
        strCityJson = doGetJson( "", strUrl, strToken );

        ObjectMapper objectMapper = new ObjectMapper( );
        objectMapper.configure( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false );
        City [ ] cities = null;
        if ( strCityJson != null && !strCityJson.isEmpty( ) )
        {
            try
            {
                cities = objectMapper.readValue( strCityJson, City [ ].class );
            }
            catch( JsonProcessingException e )
            {
                String strError = "API INSEE - Error converting to Object from JSON '" + strCityJson + "' : ";
                _logger.error( strError + e.getMessage( ), e );
            }
        }
        _logger.info( " Nb cities : " + cities.length );
        return Arrays.asList( cities );
    }

}
