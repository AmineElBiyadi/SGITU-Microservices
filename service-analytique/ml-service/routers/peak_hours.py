"""
Algorithm: Frequency-based pattern recognition. 
Normalizes validation counts per hour relative to the total volume to produce a frequency score (0-1).
Returns the top 3 hours with the highest scores as predicted peaks.
"""

from fastapi import APIRouter
from datetime import datetime
import pandas as pd
import numpy as np
from models.schemas import PeakHoursRequest, PeakHoursResponse, HourScore

router = APIRouter(prefix="/predict", tags=["predictions"])

@router.post("/peak-hours", response_model=PeakHoursResponse)
async def predict_peak_hours(request: PeakHoursRequest):
    if not request.data:
        return PeakHoursResponse(
            predicted_peak_hours=[],
            distribution=[],
            generatedAt=datetime.now()
        )

    # Load data into DataFrame
    df = pd.DataFrame([dp.dict() for dp in request.data])
    
    total_validations = df['validationCount'].sum()
    
    if total_validations == 0:
        return PeakHoursResponse(
            predicted_peak_hours=[],
            distribution=[HourScore(hour=row['hour'], score=0.0) for _, row in df.iterrows()],
            generatedAt=datetime.now()
        )

    # Normalize scores: validationCount / total_validations
    df['score'] = df['validationCount'] / total_validations
    
    # Sort by score descending
    df_sorted = df.sort_values(by='score', ascending=False)
    
    # Get top 3 hours
    top_3_hours = df_sorted.head(3)['hour'].tolist()
    
    # Create distribution list
    distribution = [
        HourScore(hour=int(row['hour']), score=float(row['score'])) 
        for _, row in df.iterrows()
    ]

    return PeakHoursResponse(
        predicted_peak_hours=top_3_hours,
        distribution=distribution,
        generatedAt=datetime.now()
    )
