import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { useNavigate } from 'react-router-dom';

const PatientProfile = () => {
  const [profile, setProfile] = useState(null);
  const [isEditing, setIsEditing] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState('');
  const [successMsg, setSuccessMsg] = useState('');
  const navigate = useNavigate();

  // Form state for allowed update fields
  const [formData, setFormData] = useState({
    fullName: '',
    phone: '',
    emergencyContact: '',
    gender: ''
  });

  // Setup Axios config to include the secure JWT token
  const getAuthConfig = () => {
    const token = localStorage.getItem('token');
    if (!token) {
      navigate('/'); // Kick out unauthenticated users
      throw new Error("No token found");
    }
    return { headers: { Authorization: `Bearer ${token}` } };
  };

  useEffect(() => {
    fetchMyProfile();
  }, []);

  const fetchMyProfile = async () => {
    setIsLoading(true);
    try {
      const res = await axios.get('http://localhost:8080/api/patients/me', getAuthConfig());
      setProfile(res.data);
      setFormData({
        fullName: res.data.fullName || '',
        phone: res.data.phone || '',
        emergencyContact: res.data.emergencyContact || '',
        gender: res.data.gender || ''
      });
      setIsLoading(false);
    } catch (err) {
      console.error(err);
      setError('Failed to load profile. Please log in again.');
      setIsLoading(false);
    }
  };

  const handleUpdate = async (e) => {
    e.preventDefault();
    setError('');
    setSuccessMsg('');
    
    try {
      const res = await axios.put('http://localhost:8080/api/patients/me', formData, getAuthConfig());
      setProfile(res.data); // Update UI with confirmed database data
      setIsEditing(false);
      setSuccessMsg('Profile updated successfully!');
      
      // Clear success message after 3 seconds
      setTimeout(() => setSuccessMsg(''), 3000);
    } catch (err) {
      setError('Failed to update profile. Please try again.');
    }
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  if (isLoading) return <div className="flex justify-center items-center h-screen text-slate-500 font-bold">Loading securely...</div>;
  if (error && !profile) return <div className="p-8 text-red-500 font-bold text-center">{error}</div>;

  return (
    <div className="min-h-screen bg-[#f8f9fc] p-8 font-sans">
      <div className="max-w-3xl mx-auto">
        
        {/* Header */}
        <div className="flex justify-between items-end mb-8">
          <div>
            <h1 className="text-3xl font-bold text-slate-800">My Medical Profile</h1>
            <p className="text-sm text-slate-500 mt-1">Manage your personal information and contacts</p>
          </div>
          <button onClick={() => navigate('/patient-dashboard')} className="text-blue-600 hover:text-blue-800 text-sm font-bold">
            &larr; Back to Dashboard
          </button>
        </div>

        {/* Success Alert */}
        {successMsg && (
          <div className="bg-green-50 border border-green-200 text-green-700 px-4 py-3 rounded-lg mb-6 font-semibold shadow-sm">
            ✓ {successMsg}
          </div>
        )}
        {error && isEditing && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg mb-6 font-semibold shadow-sm">
            {error}
          </div>
        )}

        {/* Profile Card */}
        <div className="bg-white rounded-2xl shadow-sm border border-slate-100 overflow-hidden">
          
          <div className="bg-blue-600 px-8 py-6 flex justify-between items-center">
            <div className="flex items-center gap-4">
              <div className="w-16 h-16 bg-white text-blue-600 rounded-full flex items-center justify-center text-2xl font-bold uppercase shadow-md">
                {profile?.fullName ? profile.fullName.charAt(0) : 'P'}
              </div>
              <div>
                <h2 className="text-xl font-bold text-white">{profile?.fullName}</h2>
                <p className="text-blue-100 text-sm font-medium">Patient ID: {profile?.id}</p>
              </div>
            </div>
            {!isEditing && (
              <button onClick={() => setIsEditing(true)} className="bg-white/20 hover:bg-white/30 text-white px-5 py-2 rounded-lg text-sm font-bold transition">
                Edit Profile
              </button>
            )}
          </div>

          <div className="p-8">
            {isEditing ? (
              <form onSubmit={handleUpdate} className="space-y-6">
                <div className="grid grid-cols-2 gap-6">
                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Full Name</label>
                    <input type="text" name="fullName" value={formData.fullName} onChange={handleChange} required className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-3 text-sm outline-none focus:border-blue-500 focus:bg-white transition" />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Phone Number</label>
                    <input type="tel" name="phone" value={formData.phone} onChange={handleChange} required className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-3 text-sm outline-none focus:border-blue-500 focus:bg-white transition" />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Emergency Contact</label>
                    <input type="tel" name="emergencyContact" value={formData.emergencyContact} onChange={handleChange} required className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-3 text-sm outline-none focus:border-blue-500 focus:bg-white transition" />
                  </div>
                  <div>
                    <label className="block text-xs font-bold text-slate-500 uppercase tracking-wider mb-2">Gender</label>
                    <select name="gender" value={formData.gender} onChange={handleChange} className="w-full bg-[#f4f7fe] border border-transparent rounded-lg px-4 py-3 text-sm outline-none focus:border-blue-500 focus:bg-white transition">
                      <option value="">Select...</option>
                      <option value="Male">Male</option>
                      <option value="Female">Female</option>
                      <option value="Other">Other</option>
                    </select>
                  </div>
                </div>
                
                <div className="pt-4 border-t border-slate-100 flex justify-end gap-3">
                  <button type="button" onClick={() => { setIsEditing(false); setError(''); }} className="px-6 py-2.5 rounded-lg text-sm font-bold text-slate-600 hover:bg-slate-100 transition">Cancel</button>
                  <button type="submit" className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2.5 rounded-lg text-sm font-bold shadow-sm transition">Save Changes</button>
                </div>
              </form>
            ) : (
              <div className="grid grid-cols-2 gap-y-8 gap-x-6">
                <div>
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Full Name</p>
                  <p className="text-slate-800 font-semibold text-lg">{profile?.fullName}</p>
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Phone Number</p>
                  <p className="text-slate-800 font-semibold text-lg">{profile?.phone || 'Not provided'}</p>
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Emergency Contact</p>
                  <p className="text-slate-800 font-semibold text-lg">{profile?.emergencyContact || 'Not provided'}</p>
                </div>
                <div>
                  <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Gender</p>
                  <p className="text-slate-800 font-semibold text-lg">{profile?.gender || 'Not specified'}</p>
                </div>
                
                {/* Read-Only System Fields */}
                <div className="col-span-2 pt-6 border-t border-slate-100">
                  <h3 className="text-sm font-bold text-slate-800 mb-4">Clinic Information (Read-Only)</h3>
                  <div className="grid grid-cols-3 gap-6">
                    <div className="bg-slate-50 p-4 rounded-xl">
                      <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Branch</p>
                      <p className="text-slate-700 font-semibold">{profile?.branchId || 'N/A'}</p>
                    </div>
                    <div className="bg-slate-50 p-4 rounded-xl">
                      <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Registered On</p>
                      <p className="text-slate-700 font-semibold">{profile?.registrationDate ? new Date(profile.registrationDate).toLocaleDateString() : 'N/A'}</p>
                    </div>
                    <div className="bg-slate-50 p-4 rounded-xl">
                      <p className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-1">Blood Group</p>
                      <p className="text-slate-700 font-semibold text-red-600">{profile?.bloodGroup || 'Unknown'}</p>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>

      </div>
    </div>
  );
};

export default PatientProfile;