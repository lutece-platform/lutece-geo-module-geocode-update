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
package fr.paris.lutece.plugins.geocode.daemon;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import fr.paris.lutece.plugins.geocode.service.GeoCodesINSEE;
import fr.paris.lutece.plugins.geocodes.business.City;
import fr.paris.lutece.plugins.geocodes.service.GeoCodesService;
import fr.paris.lutece.portal.service.daemon.Daemon;

public class UpdateGeocodeDaemon extends Daemon
{

    @Override
    public void run( )
    {
        updateCity( );
    }
    
    public void updateCity( )
    {
    	GeoCodesINSEE geocode = new GeoCodesINSEE( );
        List<City> lstCities = GeoCodesService.getCitiesListByLastDateUpdate( );
        for ( City city : lstCities )
        {

            Optional<City> cityOptional = geocode.getCityByDateAndCode( city.getDateValidityStart( ), city.getCode( ) );
            if ( cityOptional.isPresent( ) )
            {
                City cityINSEE = cityOptional.get( );
                city.setValueMin( cityINSEE.getValueMin( ) );
                city.setValueMinComplete( cityINSEE.getValueMinComplete( ) );
                if ( cityINSEE.getDateValidityEnd( ) != null )
                {
                    city.setDateValidityEnd( cityINSEE.getDateValidityEnd( ) );
                }
                city.setDateLastUpdate( new Date( System.currentTimeMillis( ) ) );
                GeoCodesService.updateCity( city );
            }
        }

    }
}
