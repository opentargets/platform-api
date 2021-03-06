import { gql } from 'apollo-server-express';

import { evidencePathways } from '../../../apis/openTargets';

export const id = 'pathways';

export const summaryTypeDefs = gql`
  type EvidenceSummaryPathways {
    pathwayCount: Int!
    sources: [Source!]!
  }
`;

export const summaryResolvers = {
  EvidenceSummaryPathways: {
    pathwayCount: ({ _ensgId, _efoId }) =>
      evidencePathways(_ensgId, _efoId).then(
        ({ pathwayCount }) => pathwayCount
      ),
    sources: () => [
      {
        name: 'Reactome',
        url:
          'https://docs.targetvalidation.org/data-sources/affected-pathways#reactome',
      },
      {
        name: 'SLAPenrich',
        url:
          'https://docs.targetvalidation.org/data-sources/affected-pathways#slapenrich',
      },
      {
        name: 'PROGENy',
        url:
          'https://docs.targetvalidation.org/data-sources/affected-pathways#progeny',
      },
    ],
  },
};

export const sectionTypeDefs = gql`
  enum ReactomeActivity {
    DECREASED_TRANSCRIPT_LEVEL
    GAIN_OF_FUNCTION
    LOSS_OF_FUNCTION
    PARTIAL_LOSS_OF_FUNCTION
    UP_OR_DOWN
  }
  type EvidenceRowPathways {
    activity: ReactomeActivity
    disease: Disease!
    pathway: ReactomePathway!
    mutations: [String!]!
    source: Source!
  }
  type EvidenceDetailPathways {
    rowsPathways: [EvidenceRowPathways!]!
  }
`;

export const sectionResolvers = {
  EvidenceDetailPathways: {
    rowsPathways: ({ _ensgId, _efoId }) =>
      evidencePathways(_ensgId, _efoId).then(
        ({ rowsPathways }) => rowsPathways
      ),
  },
};
