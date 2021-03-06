import { gql } from 'apollo-server-express';

import { evidenceIntogen } from '../../../apis/openTargets';

export const id = 'intogen';

export const summaryTypeDefs = gql`
  type EvidenceSummaryIntogen {
    hasMutations: Boolean!
    sources: [Source!]!
  }
`;

export const summaryResolvers = {
  EvidenceSummaryIntogen: {
    hasMutations: ({ _ensgId, _efoId }) =>
      evidenceIntogen(_ensgId, _efoId).then(({ hasMutations }) => hasMutations),
    sources: () => [
      {
        name: 'IntOGen',
        url:
          'https://docs.targetvalidation.org/data-sources/somatic-mutations#intogen',
      },
    ],
  },
};

export const sectionTypeDefs = gql`
  enum IntogenActivity {
    GAIN_OF_FUNCTION
    LOSS_OF_FUNCTION
    UNKNOWN
  }
  enum InheritancePattern {
    X_LINKED_RECESSIVE
    DOMINANT_OR_RECESSIVE
    DOMINANT
    RECESSIVE
    UNKNOWN
  }
  type EvidenceRowIntogen {
    disease: Disease!
    activity: IntogenActivity!
    inheritancePattern: InheritancePattern!
    source: Source!
    pmId: String!
    pval: Float!
    mutationMetrics: MutationMetrics!
    cohort: Cohort!
    analysisMethods: [String]!
  }
  type EvidenceDetailIntogen {
    rows: [EvidenceRowIntogen!]!
  }
  type Cohort {
    name: String!
    description: String!
  }
  type MutationMetrics {
    value: Int
    total: Int
  }
`;

export const sectionResolvers = {
  EvidenceDetailIntogen: {
    rows: ({ _ensgId, _efoId }) =>
      evidenceIntogen(_ensgId, _efoId).then(({ rows }) => rows),
  },
};
