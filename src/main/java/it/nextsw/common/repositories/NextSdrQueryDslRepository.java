package it.nextsw.common.repositories;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.EntityPath;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.DatePath;
import com.querydsl.core.types.dsl.DateTimePath;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberPath;
import com.querydsl.core.types.dsl.StringPath;
import it.nextsw.common.repositories.exceptions.InvalidFilterException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.persistence.Column;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.querydsl.binding.QuerydslBinderCustomizer;
import org.springframework.data.querydsl.binding.QuerydslBindings;

/**
 * QuerydslBinderCustomizer: customizza i filtri
 *
 * @author gdm
 * @param <E>
 * @param <ID>
 * @param <T>
 */
public interface NextSdrQueryDslRepository<E extends Object, ID extends Object, T extends EntityPath<?>>
        extends QuerydslBinderCustomizer<T>,
        QuerydslPredicateExecutor<E> {

    /**
     * per generare il Q per fare i filtri, si istanzia un oggetto del campo di
     * ricerca. Implemetando il metodo customize ci si gestisce i filtri come si
     * vuole.
     *
     *
     * @param bindings
     * @param entityPath
     */
    @Override
    default void customize(QuerydslBindings bindings, T entityPath) {

        bindings.bind(LocalDate.class).all(
                (
                        final Path<LocalDate> path,
                        final Collection<? extends LocalDate> values) -> {
                    final List<? extends LocalDate> dates = new ArrayList<>(values);
                    Predicate res;
                    if (values.size() == 1) {
                        DatePath datePath = (DatePath) path;
                        res = datePath.eq(dates.get(0));
                    } else if (dates.size() == 2) {
                        Collections.sort(dates);
                        DatePath datePath = (DatePath) path;
                        res = datePath.goe(dates.get(0)).and(datePath.loe(dates.get(1)));
                    } else {
                        res = Expressions.asBoolean(true).isTrue();
                    }
                    return Optional.of(res);
                });

        bindings.bind(LocalDateTime.class).all(
                (
                        final Path<LocalDateTime> path,
                        final Collection<? extends LocalDateTime> values) -> {
                    final List<? extends LocalDateTime> dates = new ArrayList<>(values);
                    Predicate res;
                    if (values.size() == 1) {
                        DateTimePath dateTimePath = (DateTimePath) path;
                        if (dates.get(0).toLocalDate().atTime(0, 0, 0).equals(LocalDateTime.of(9999, Month.JANUARY, 1, 0, 0, 0))) {
                            res = dateTimePath.isNull();
                        } else if (dates.get(0).toLocalDate().atTime(0, 0, 0).equals(LocalDateTime.of(9998, Month.JANUARY, 1, 0, 0, 0))) {
                            res = dateTimePath.isNotNull();
                        } else {
                            dateTimePath = (DateTimePath) path;
                            LocalDateTime startDate = dates.get(0).toLocalDate().atTime(0, 0, 0);
                            LocalDateTime endDate = startDate.plusDays(1);
                            res = dateTimePath.goe(startDate).and(dateTimePath.lt(endDate));
                        }
                    } else if (dates.size() == 2) {
                        Collections.sort(dates);
                        DateTimePath dateTimePath = (DateTimePath) path;
                        LocalDateTime startDate = dates.get(0).toLocalDate().atTime(0, 0, 0);
                        LocalDateTime endDate = dates.get(1).toLocalDate().atTime(0, 0, 0).plusDays(1);
                        res = dateTimePath.goe(startDate).and(dateTimePath.lt(endDate));
                    } else {
                        res = Expressions.asBoolean(true).isTrue();
                    }
                    return Optional.of(res);
                });
        
        bindings.bind(Enum.class).first((path, value) -> {
            System.out.println("dentro");
            return null; //To change body of generated lambdas, choose Tools | Templates.
        });

        bindings.bind(String.class).all(
                (StringPath path, Collection<? extends String> values) -> {
                    final List<? extends Object> strings = new ArrayList<>(values);
                    Predicate res;
                    try {
                        if (values.isEmpty()) {
                            res = Expressions.asBoolean(true).isTrue();
                        } else {
                            /* gestione dei campi tsvector:
                             * se sulla colonna dell'entità c'è l'annotazione Column e in columnDefinition contiene tsvector allora mi trovo in un campo tsvector
                             * in quel caso la ricerca viene effettuata secondo la metodologia di ricerca ts di postgres. Viene richiamata la funzione fts_match
                             *  che viene registrata creando in CustomDialect per hibernate (classe it.nextsw.common.dialect.CustomPostgresDialect).
                             * Per il corretto funzionamento è necessatio abilitare il CustomDialect aggiungendo la seguente riga nell'application.properties del progetto:
                             *  "spring.jpa.properties.hibernate.dialect=it.nextsw.common.dialect.CustomPostgresDialect"
                            */
                            String columDefinition = path.getAnnotatedElement().getAnnotation(Column.class).columnDefinition();
                            if (columDefinition != null && columDefinition.contains("tsvector")) {
                                BooleanExpression booleanTemplate = Expressions.booleanTemplate(
                                        String.format("FUNCTION('fts_match', italian, {0}, '%s')= true", String.join(" ", (List<String>)strings)), 
                                        path
                                ); 
                                res = booleanTemplate;
                            } 
//                            else if (columDefinition != null && columDefinition.contains("jsonb")) {
//                                if (strings.size() == 1) {
//                                    BooleanExpression booleanTemplate = Expressions.booleanTemplate(
//                                        String.format("FUNCTION('jsonb_match', {0}, '%s')= true", strings.get(0)), 
//                                        path
//                                    ); 
//                                res = booleanTemplate;
//                                } else {
//                                    BooleanBuilder b = new BooleanBuilder();
//                                    for (Object value: strings) {
//                                        BooleanExpression booleanTemplate = Expressions.booleanTemplate(
//                                            String.format("FUNCTION('jsonb_match', {0}, '%s')= true", value,
//                                            path
//                                            ));
//                                        b = b.or(booleanTemplate);
//                                    }
//                                    res = b;
//                                }
//                            } 
                            else {
                                if (values.size() == 1) {
                                    String string;
                                    if (strings.get(0).getClass().isEnum()) {
                                        string = ((Enum) strings.get(0)).toString();
                                        res = path.eq(string);
                                    } else {
                                        string = (String) strings.get(0);
                                        res = getStringPredicate(string, path);
                                    }
                                } else {
                                    BooleanBuilder b = new BooleanBuilder();
                                    for (Object value : values) {
                                        if (value.getClass().isEnum()) {
                                            String string = ((Enum) value).toString();
                                            b = b.or( path.eq(string));
                                        } else {
                                            b = b.or(getStringPredicate((String) value, path));
                                        }
                                    }
                                    res = b;
                                }
                            }
                        }
                        return Optional.of(res);
                        
                    } catch (InvalidFilterException ex) {
                        return Optional.of(Expressions.asBoolean(true).isTrue());
                    }
                });

        bindings.bind(Integer.class).all(
                (Path<Integer> path, Collection<? extends Integer> values) -> {
                    final List<? extends Integer> numbers = new ArrayList<>(values);
                    Predicate res = null;
                    if (values.isEmpty()) {
                        res = Expressions.asBoolean(true).isTrue();
                    } else if (values.size() == 1) {
                        // stratagemma per riuscire a filtrare per null.
                        // siccome, se passo null da errore perchè non è un numero, interpreto 999999999 come null
                        if (numbers.get(0) == 999999999) {
                            NumberPath integerPath = (NumberPath) path;
                            res = integerPath.isNull();
                        } else {
                            NumberPath integerPath = (NumberPath) path;
                            res = integerPath.eq(numbers.get(0));
                        }
                    } else {
                        BooleanBuilder b = new BooleanBuilder();
                        for (Integer value : values) {
                            NumberPath integerPath = (NumberPath) path;
                            b = b.or(integerPath.eq(value));
                        }
                        res = b;
                    }
                    return Optional.of(res);
                });

    }

    default StringOperation getStringOperation(String valueToParse) throws InvalidFilterException {
        StringOperation res;

        /**
         * esempio: nome=$equalsIgnorecase(NOME). I valori di filtro permessi
         * sono: contains, containsIgnoreCase, startsWith, startsWithIgnoreCase,
         * equals, equalsIgnoreCase
         */
        String regex = "\\$(.*?)\\((.*)\\)";
        Pattern r = Pattern.compile(regex);
        Matcher m = r.matcher(valueToParse);

        if (m.find()) {
            try {
                String operation = m.group(1);
                String value = m.group(2);
                res = new StringOperation(StringOperation.Operators.valueOf(operation), value);
            } catch (Exception ex) {
                throw new InvalidFilterException("filtro non valido", ex);
            }
        } else {
            throw new InvalidFilterException("filtro non valido");
        }

        return res;
    }

    default Predicate getStringPredicate(String valueToParse, StringPath stringPath) throws InvalidFilterException {

        BooleanExpression res;

        StringOperation stringOperation = getStringOperation(valueToParse);

        switch (stringOperation.getOperator()) {
            case contains:
                res = stringPath.contains(stringOperation.getValue());
                break;
            case containsIgnoreCase:
                res = stringPath.containsIgnoreCase(stringOperation.getValue());
                break;
            case startsWith:
                res = stringPath.startsWith(stringOperation.getValue());
                break;
            case startsWithIgnoreCase:
                res = stringPath.startsWithIgnoreCase(stringOperation.getValue());
                break;
            case equals:
                res = stringPath.eq(stringOperation.getValue());
                break;
            case equalsIgnoreCase:
                res = stringPath.equalsIgnoreCase(stringOperation.getValue());
                break;
            default:
                throw new InvalidFilterException(String.format("operatore %s non valido", stringOperation.getOperator()));

        }
        return res;
    }

}
