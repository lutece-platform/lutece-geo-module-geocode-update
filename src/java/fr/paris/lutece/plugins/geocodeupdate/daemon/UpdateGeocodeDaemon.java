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

import fr.paris.lutece.plugins.geocodes.business.City;
import fr.paris.lutece.plugins.geocodes.business.GeocodesChangesStatusEnum;
import fr.paris.lutece.plugins.geocodes.service.GeoCodesService;
import fr.paris.lutece.plugins.geocodeupdate.business.CityINSEE;
import fr.paris.lutece.plugins.geocodeupdate.service.GeoCodesINSEE;
import fr.paris.lutece.portal.service.daemon.Daemon;
import fr.paris.lutece.portal.service.util.AppLogService;
import org.apache.commons.lang3.StringUtils;

public class UpdateGeocodeDaemon extends Daemon
{
    private static final String CONSTANTE_CODE_COUNTRY = "99100";
    private static final String CONSTANTE_DATE_MAX = "2999-12-31";
    private static final String CONSTANTE_DATE_FORMAT = "yyyy-MM-dd";

    @Override
    public void run( )
    {
        GeoCodesINSEE geocode = new GeoCodesINSEE( );
        List<City> lstCities = GeoCodesService.getCitiesListByLastDateUpdate( );
        List<CityINSEE> cityINSEEList = geocode.getAllCities( );

        for( CityINSEE cityINSEE : cityINSEEList )
        {
            boolean knownCity = false;
            for ( City city : lstCities )
            {
                if ( cityINSEE.getCode( ).equals( city.getCode( ) )
                        && this.areDateSameDay( cityINSEE.getDateValidityStart(), city.getDateValidityStart( ) ) )
                {
                    knownCity = true;
                    this.updateCity(city, cityINSEE);
                    break;
                }
            }
            if(!knownCity)
            {
                this.createCity(cityINSEE);
            }
        }
    }

    private void updateCity( City city, CityINSEE cityINSEE )
    {
        boolean changes = false;

        if (!StringUtils.equals(city.getCodeCountry(), cityINSEE.getCodeCountry())
            && cityINSEE.getCodeCountry() != null)
        {
            city.setCodeCountry(cityINSEE.getCodeCountry());
            changes = true;
        }
        if (!StringUtils.equals(city.getCode(), cityINSEE.getCode())
        && cityINSEE.getCode() != null)
        {
            city.setCode(cityINSEE.getCode());
            changes = true;
        }
        if (!StringUtils.equals(city.getValue(), cityINSEE.getValue())
        && cityINSEE.getValue() != null)
        {
            city.setValue(cityINSEE.getValue());
            changes = true;
        }
        if (!StringUtils.equals(city.getCodeZone(), cityINSEE.getCodeZone())
        && cityINSEE.getCodeZone() != null)
        {
            city.setCodeZone(cityINSEE.getCodeZone());
            changes = true;
        }
        if (cityINSEE.getDateValidityStart() != null && !this.areDateSameDay( cityINSEE.getDateValidityStart(), city.getDateValidityStart( ) ))
        {
            city.setDateValidityStart(cityINSEE.getDateValidityStart());
            changes = true;
        }
        if (cityINSEE.getDateValidityEnd() != null && !this.areDateSameDay( cityINSEE.getDateValidityEnd(), city.getDateValidityEnd( ) ))
        {
            city.setDateValidityEnd(cityINSEE.getDateValidityEnd());
            changes = true;
            AppLogService.debug("Date de fin mise à jour pour " + city.getValueMin() + " et date de fin : " + city.getDateValidityEndToString());
        }
        if (!StringUtils.equals(city.getValueMin(), cityINSEE.getValueMin())
        && cityINSEE.getValueMin() != null)
        {
            city.setValueMin(cityINSEE.getValueMin());
            changes = true;
        }
        if (!StringUtils.equals(city.getValueMinComplete(), cityINSEE.getValueMinComplete())
        && cityINSEE.getValueMinComplete() != null)
        {
            city.setValueMinComplete(cityINSEE.getValueMinComplete());
            changes = true;
        }

        if (changes)
        {
            if (!GeoCodesService.checkChangesExistence( city ))
            {
                city.setDateLastUpdate(new Date(System.currentTimeMillis()));
                GeoCodesService.addCityChanges(city, this.getClass().getSimpleName(), GeocodesChangesStatusEnum.PENDING.name());
            }
            else
            {
                GeoCodesService.updateCityChanges(city, this.getClass().getSimpleName(), GeocodesChangesStatusEnum.PENDING.name());
            }
        }
    }
    
    private void createCity( CityINSEE cityINSEE )
    {
        City newCity = new City( );
        newCity.setDateLastUpdate( new Date( System.currentTimeMillis( ) ) );
        newCity.setCode( cityINSEE.getCode( ) );
        newCity.setCodeCountry( CONSTANTE_CODE_COUNTRY );
        newCity.setValue ( cityINSEE.getValueMin( ).toUpperCase( ) );
        newCity.setValueMin ( cityINSEE.getValueMin( ) );
        newCity.setValueMinComplete( cityINSEE.getValueMinComplete( ) );
        newCity.setCodeZone( cityINSEE.getCode( ).substring( 0, 2 ) );
        newCity.setDateValidityStart( cityINSEE.getDateValidityStart( ) );
        SimpleDateFormat dateFormat = new SimpleDateFormat( CONSTANTE_DATE_FORMAT );
        try {
            newCity.setDateValidityEnd( dateFormat.parse( CONSTANTE_DATE_MAX ) );
        } catch (ParseException e) {
            AppLogService.error( e.getMessage(  ), e );
        }
        if (!GeoCodesService.checkChangesExistence( newCity ))
        {
            GeoCodesService.createCity(newCity, this.getClass().getSimpleName(), GeocodesChangesStatusEnum.PENDING.name());
        }
        else
        {
            GeoCodesService.updateCityChanges(newCity, this.getClass().getSimpleName(), GeocodesChangesStatusEnum.PENDING.name());
        }
        AppLogService.debug("New city created : " + cityINSEE.getCode() + " name : " + cityINSEE.getValueMin() + " start date : "
                + cityINSEE.getDateValidityStartToString());
    }

    private boolean areDateSameDay(Date date1, Date date2)
    {
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        return fmt.format(date1).equals(fmt.format(date2));
    }
}
