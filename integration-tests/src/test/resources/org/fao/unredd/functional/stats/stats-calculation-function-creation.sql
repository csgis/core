-- Function: clasif_bak.cobertura(text, text, text, text, text, text)

-- DROP FUNCTION $schema.cobertura(text, text, text, text, text, text);

CREATE OR REPLACE FUNCTION calculo_cobertura(IN fajas_table_name text, IN divisions_table_name text, IN division_id_field_name text, IN classification_table_name text, IN class_field_name text, IN date_field_name text)
  RETURNS TABLE(division_id character varying, class character varying, fecha_result date, ha real) AS
$BODY$
DECLARE
	faja RECORD;
	faja_geom geometry;
BEGIN
	DROP TABLE IF EXISTS tmp_areas;
	CREATE TEMP TABLE tmp_areas (division_id varchar, class varchar,fecha date, ha real);
    
	FOR faja IN EXECUTE format('SELECT * FROM %s', fajas_table_name) LOOP

		RAISE NOTICE 'procesando faja %', faja.srid;

		faja_geom := ST_Transform(faja.geom, faja.srid);

		-- Transformamos las divisiones y la clasificacion al CRS de la faja
		RAISE NOTICE 'proyectando al crs de la faja';
		EXECUTE format('CREATE TABLE classification_projected AS SELECT %s as clase, %s as fecha, ST_Transform(geom, %s) AS geom FROM %s', class_field_name,date_field_name, faja.srid, classification_table_name);
		EXECUTE format('CREATE TABLE divisions_projected AS      SELECT %s as    id, ST_Transform(geom, %s) AS geom FROM %s', division_id_field_name, faja.srid, divisions_table_name);

		RAISE NOTICE 'cortando clasificación y divisiones con geometría de la faja';


		-- Cortampos las divisiones y la clasificación (ya proyectadas) con la geometría de la faja proyectada al SRID correspondiente
		CREATE TABLE classification_cut AS SELECT clase,fecha, ST_Intersection(geom, faja_geom) AS geom FROM classification_projected WHERE ST_Intersects(geom, faja_geom);
		CREATE TABLE divisions_cut AS      SELECT    id, ST_Intersection(geom, faja_geom) AS geom FROM divisions_projected      WHERE ST_Intersects(geom, faja_geom);

		RAISE NOTICE 'intersectando divisiones y clasificacion';
		-- Intersectamos divisiones y clasificacio
		CREATE TABLE intersection AS SELECT ST_Intersection(c.geom, d.geom) AS geom, d.id, c.clase, c.fecha FROM classification_cut c, divisions_cut d WHERE ST_Intersects(c.geom, d.geom);

		RAISE NOTICE 'acumulando areas';
		-- Calculamos el área para cada geometría
		INSERT INTO tmp_areas (division_id, class, fecha, ha) SELECT id, clase, fecha, ST_Area(geom) / 10000 AS ha FROM intersection;

		-- Eliminamos las tablas
		DROP TABLE classification_projected, divisions_projected, classification_cut, divisions_cut, intersection;
	END LOOP;

	-- Devolvemos la suma del área de las geometrías para cada departamento y clasificación de cobertura
	RETURN QUERY SELECT ta.division_id, ta.class, ta.fecha, sum(ta.ha) as ha FROM tmp_areas ta GROUP BY ta.division_id, ta.fecha, ta.class ORDER BY ta.division_id, ta.class, ta.fecha;
END;
$BODY$
  LANGUAGE plpgsql VOLATILE
  COST 100
  ROWS 1000;
