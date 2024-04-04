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
package fr.paris.lutece.plugins.geocodeupdate.daemon;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import fr.paris.lutece.plugins.geocodes.business.City;
import fr.paris.lutece.plugins.geocodes.service.GeoCodesLocal;
import fr.paris.lutece.plugins.geocodes.service.GeoCodesService;
import fr.paris.lutece.plugins.geocodeupdate.business.CityINSEE;
import fr.paris.lutece.plugins.geocodeupdate.service.GeoCodesINSEE;
import fr.paris.lutece.portal.service.daemon.Daemon;
import fr.paris.lutece.portal.service.util.AppLogService;

public class UpdateNewCityGeocodeDaemon extends Daemon
{
    public static final String CONSTANTE_CODE_COUNTRY = "99100";
    public static final String CONSTANTE_DATE_MAX = "2999-12-31";
    public static final String CONSTANTE_DATE_FORMAT = "yyyy-MM-dd";

    @Override
    public void run( )
    {
        GeoCodesINSEE geocodeINSEE = new GeoCodesINSEE( );
        GeoCodesLocal geocodeLocal = new GeoCodesLocal( );
        List<CityINSEE> lstAllCities = geocodeINSEE.getAllCities( );
        for ( CityINSEE newCity : lstAllCities )
        {
            Optional<City> optCityLocal = geocodeLocal.getCityByDateAndCode( new Date( System.currentTimeMillis( ) ), newCity.getCode( ) );
            if ( optCityLocal.isEmpty( ) )
            {
                City newCityINSEE = new City( );
                newCityINSEE.setDateLastUpdate( new Date( System.currentTimeMillis( ) ) );
                newCityINSEE.setCode( newCity.getCode( ) );
                newCityINSEE.setCodeCountry( CONSTANTE_CODE_COUNTRY );
                newCityINSEE.setValue ( newCity.getValueMin( ).toUpperCase( ) );
                newCityINSEE.setValueMin ( newCity.getValueMin( ) );
                newCityINSEE.setValueMinComplete( newCity.getValueMinComplete( ) );
                newCityINSEE.setCodeZone( newCity.getCode( ).substring( 0, 2 ) );
                newCityINSEE.setDateValidityStart( newCity.getDateValidityStart( ) );
                SimpleDateFormat dateFormat = new SimpleDateFormat( CONSTANTE_DATE_FORMAT );
                try {
                	newCityINSEE.setDateValidityEnd( dateFormat.parse( CONSTANTE_DATE_MAX ) );
				} catch (ParseException e) {
					AppLogService.error( e.getMessage(  ), e );
				}
                GeoCodesService.createCity( newCityINSEE );
                AppLogService.debug( "New city created : " + newCity.getCode( ) + " name : " + newCity.getValueMin( ) + " start date : "
                        + newCity.getDateValidityStartToString( ) );
            }

        }

    }

}
