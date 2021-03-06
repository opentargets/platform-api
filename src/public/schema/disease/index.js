import { gql } from 'apollo-server-express';
// import { print } from 'graphql/language/printer';
import _ from 'lodash';

import { diseaseTargetsConnection } from '../../apis/openTargets';
import therapeuticAreasPerDisease from './therapeuticAreasPerDisease';

// load targets connection
import {
  typeDefs as targetsConnectionTypeDefs,
  resolvers as targetsConnectionResolvers,
} from './targetsConnection';

// load sections
import * as sectionsObject from './sectionIndex';
const sections = Object.values(sectionsObject);

// combine type defs
const summaryTypeDefs = sections.map(d => d.summaryTypeDefs);
const sectionTypeDefs = sections.map(d => d.sectionTypeDefs);
const summariesTypeDef = gql`
  type DiseaseSummaries {
    ${sections
      .map(d => `${d.id}: DiseaseSummary${_.upperFirst(d.id)}`)
      .join('\n')}
  }
`;
const sectionsTypeDef = gql`
  type DiseaseDetails {
    ${sections
      .map(d => `${d.id}: DiseaseDetail${_.upperFirst(d.id)}`)
      .join('\n')}
  }
`;
const diseaseTypeDef = gql`
  type Disease {
    id: String!
    name: String!
    description: String!
    synonyms: [String!]!
    therapeuticAreas: [Disease!]!
    summaries: DiseaseSummaries!
    details: DiseaseDetails!
    targetsConnection(
      facets: DiseaseTargetsConnectionFacetsInput
      sortBy: DiseaseTargetsConnectionSortByInput
      first: Int
      after: String
      search: String
    ): DiseaseTargetsConnection!
  }
`;
export const typeDefs = [
  ...summaryTypeDefs,
  ...sectionTypeDefs,
  summariesTypeDef,
  sectionsTypeDef,
  ...targetsConnectionTypeDefs,
  diseaseTypeDef,
];

// merge resolvers
const summariesResolvers = sections.map(d => d.summaryResolvers);
const sectionsResolvers = sections.map(d => d.sectionResolvers);
const summariesResolver = {
  DiseaseSummaries: sections.reduce((acc, d) => {
    acc[d.id] = _.identity;
    return acc;
  }, {}),
};
const sectionsResolver = {
  DiseaseDetails: sections.reduce((acc, d) => {
    acc[d.id] = _.identity;
    return acc;
  }, {}),
};
const diseaseResolver = {
  Disease: {
    id: ({ _efoId, id }, args, { diseaseLoader }) =>
      id ? id : diseaseLoader.load(_efoId).then(({ id }) => id),
    name: ({ _efoId, name }, args, { diseaseLoader }) =>
      name ? name : diseaseLoader.load(_efoId).then(({ name }) => name),
    description: ({ _efoId, description }, args, { diseaseLoader }) =>
      description
        ? description
        : diseaseLoader.load(_efoId).then(({ description }) => description),
    synonyms: ({ _efoId, synonyms }, args, { diseaseLoader }) =>
      synonyms
        ? synonyms
        : diseaseLoader.load(_efoId).then(({ synonyms }) => synonyms),
    therapeuticAreas: ({ _efoId }, args, { diseaseLoader }) =>
      therapeuticAreasPerDisease[_efoId]
        ? therapeuticAreasPerDisease[_efoId].map(({ id, name }) => ({
            _efoId: id,
            id,
            name,
          }))
        : [],
    summaries: _.identity,
    details: _.identity,
    targetsConnection: (
      { _efoId },
      { facets, sortBy, search = '', first = 50, after = null }
    ) => {
      const { field: sortField, ascending: sortAscending = false } =
        sortBy || {};
      return diseaseTargetsConnection(
        _efoId,
        facets,
        search,
        sortField,
        sortAscending,
        first,
        after
      ).then(data => ({
        _efoId,
        _data: data,
      }));
    },
  },
};
export const resolvers = _.merge(
  ...summariesResolvers,
  ...sectionsResolvers,
  summariesResolver,
  sectionsResolver,
  targetsConnectionResolvers,
  diseaseResolver
);
